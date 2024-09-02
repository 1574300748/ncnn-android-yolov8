// 先定义返回的数据
#include <jni.h>  // 用于JNI相关的类型和函数
#include "yolo.h"  // 假设存在Yolo类的定义
#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON
static Yolo* g_yolo = 0;
static jclass objCls = NULL;
static jmethodID constructortorId;
static jfieldID xId;
static jfieldID yId;
static jfieldID wId;
static jfieldID hId;
static jfieldID labelId;
static jfieldID probId;

static ncnn::Mutex lock;

extern "C"
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_YoloAPI_Init(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{

    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
    __android_log_print(ANDROID_LOG_ERROR,"YoloAPI","modelid or cpugpu invaild");
    __android_log_print(ANDROID_LOG_ERROR,"YoloAPI","modelid: %d",modelid);
    __android_log_print(ANDROID_LOG_ERROR,"YoloAPI","cpugpu: %d",cpugpu);
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_ERROR,"YoloAPI", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
            "n",
            "s",
    };

    const int target_sizes[] =
    {
    320,
    320,
    };

    const float mean_vals[][3] =
    {
    {103.53f, 116.28f, 123.675f},
    {103.53f, 116.28f, 123.675f},
    };

    const float norm_vals[][3] =
    {
    { 1 / 255.f, 1 / 255.f, 1 / 255.f },
    { 1 / 255.f, 1 / 255.f, 1 / 255.f },
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
    ncnn::MutexLockGuard g(lock);

    if (use_gpu && ncnn::get_gpu_count() == 0)
    {
    // no gpu
    delete g_yolo;
    g_yolo = 0;
    }
    else
    {
        if (!g_yolo)
            g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
    }
}

    jclass localObjCls = env->FindClass("com/tencent/yolov8ncnn/YoloAPI$Obj");   // 注意名称要对应
    objCls = reinterpret_cast<jclass>(env->NewGlobalRef(localObjCls));
    if (objCls == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "YoloAPI", "Failed to find objCls");
    return NULL; // 或者其他适当的错误处理
    }else{
    __android_log_print(ANDROID_LOG_ERROR, "YoloAPI", "succeed to find objCls");
    }

    constructortorId = env->GetMethodID(objCls, "<init>", "(Lcom/tencent/yolov8ncnn/YoloAPI;)V");  // 注意名称要对应
    if(constructortorId==NULL){
    __android_log_print(ANDROID_LOG_ERROR, "YoloAPI", "constructortorId is null");
    }
    xId = env->GetFieldID(objCls, "x", "F");
    yId = env->GetFieldID(objCls, "y", "F");
    wId = env->GetFieldID(objCls, "w", "F");
    hId = env->GetFieldID(objCls, "h", "F");
    labelId = env->GetFieldID(objCls, "label", "I");
    probId = env->GetFieldID(objCls, "prob", "F");

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_tencent_yolov8ncnn_YoloAPI_Detect(JNIEnv *env, jobject thiz, jobject bitmap,
jboolean use_gpu){
double start_time = ncnn::get_current_time();
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    const int width = info.width;
    const int height = info.height;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    return NULL;
    std::vector<Object> objects;
if (g_yolo)
{
// 这里调用了yolo.cpp的新函数，将在下面步骤中定义
    float prob_threshold = 0.5f;
    float nms_threshold = 0.4f;
    g_yolo->detectPicture(env, bitmap, width, height, objects,prob_threshold,nms_threshold);
}
else
{
__android_log_print(ANDROID_LOG_DEBUG, "YoloAPI", "g_yolo is NULL!请先确保初始化成功");
}
// 在detectPicture方法中将结果保存在了 objects 中，还需继续对他进行转换
jobjectArray jObjArray = env->NewObjectArray(objects.size(), objCls, NULL);

__android_log_print(ANDROID_LOG_DEBUG, "YoloAPI", "%u  objects detected!", objects.size());

for (size_t i=0; i<objects.size(); i++)
{
jobject jObj = env->NewObject(objCls, constructortorId, thiz);

env->SetFloatField(jObj, xId, objects[i].rect.x);
env->SetFloatField(jObj, yId, objects[i].rect.y);
env->SetFloatField(jObj, wId, objects[i].rect.width);
env->SetFloatField(jObj, hId, objects[i].rect.height);
env->SetIntField(jObj, labelId, objects[i].label);
env->SetFloatField(jObj, probId, objects[i].prob);

env->SetObjectArrayElement(jObjArray, i, jObj);
}

double elasped = ncnn::get_current_time() - start_time;
__android_log_print(ANDROID_LOG_DEBUG, "YoloAPI", "the entire detection takes %.2fms", elasped);

return jObjArray;
}
