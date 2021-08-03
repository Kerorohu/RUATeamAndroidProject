//
// Created by hyx_4 on 2021/6/10.
//
#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>
#include <android/bitmap.h>
#include <android/log.h>
#include <vector>
//#include <string>
using namespace cv;
#define ASSERT(status, ret)     if (!(status)) { return ret; }
#define ASSERT_FALSE(status)    ASSERT(status, false)

extern "C" {
bool BitmapToMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    void *bitmapPixels;                                            // 保存图片像素数据
    AndroidBitmapInfo bitmapInfo;                                   // 保存图片参数

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0);        // 获取图片参数
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format ==
                    ANDROID_BITMAP_FORMAT_RGB_565);          // 只支持 ARGB_8888 和 RGB_565
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0);  // 获取图片像素（锁定内存块）
    ASSERT_FALSE(bitmapPixels);

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);    // 建立临时 mat
        tmp.copyTo(matrix);                                                         // 拷贝到目标 matrix
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_BGR5652RGB);
    }

    AndroidBitmap_unlockPixels(env, obj_bitmap);            // 解锁
    return true;
}

void codeRotateByZ(double x, double y, double thetaz, double &outx, double &outy) {
    double x1 = x;//将变量拷贝一次，保证&x == &outx这种情况下也能计算正确
    double y1 = y;
    double rz = thetaz * CV_PI / 180;
    outx = cos(rz) * x1 - sin(rz) * y1;
    outy = sin(rz) * x1 + cos(rz) * y1;
}

//将空间点绕Y轴旋转
//输入参数 x z为空间点原始x z坐标
//thetay为空间点绕Y轴旋转多少度，角度制范围在-180到180
//outx outz为旋转后的结果坐标
void codeRotateByY(double x, double z, double thetay, double &outx, double &outz) {
    double x1 = x;
    double z1 = z;
    double ry = thetay * CV_PI / 180;
    outx = cos(ry) * x1 + sin(ry) * z1;
    outz = cos(ry) * z1 - sin(ry) * x1;
}

//将空间点绕X轴旋转
//输入参数 y z为空间点原始y z坐标
//thetax为空间点绕X轴旋转多少度，角度制，范围在-180到180
//outy outz为旋转后的结果坐标
void codeRotateByX(double y, double z, double thetax, double &outy, double &outz) {
    double y1 = y;//将变量拷贝一次，保证&y == &y这种情况下也能计算正确
    double z1 = z;
    double rx = thetax * CV_PI / 180;
    outy = cos(rx) * y1 - sin(rx) * z1;
    outz = cos(rx) * z1 + sin(rx) * y1;
}

bool MatrixToBitmap(JNIEnv *env, cv::Mat &matrix, jobject obj_bitmap) {
    void *bitmapPixels;                                            // 保存图片像素数据
    AndroidBitmapInfo bitmapInfo;                                   // 保存图片参数

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0);        // 获取图片参数
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format ==
                    ANDROID_BITMAP_FORMAT_RGB_565);          // 只支持 ARGB_8888 和 RGB_565
    ASSERT_FALSE(matrix.dims == 2
                 && bitmapInfo.height == (uint32_t) matrix.rows
                 && bitmapInfo.width == (uint32_t) matrix.cols);                   // 必须是 2 维矩阵，长宽一致
    ASSERT_FALSE(matrix.type() == CV_8UC1 || matrix.type() == CV_8UC3 || matrix.type() == CV_8UC4);
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0);  // 获取图片像素（锁定内存块）
    ASSERT_FALSE(bitmapPixels);

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2RGBA);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2RGBA);
                break;
            case CV_8UC4:
                matrix.copyTo(tmp);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2BGR565);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2BGR565);
                break;
            case CV_8UC4:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGBA2BGR565);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
    }
    AndroidBitmap_unlockPixels(env, obj_bitmap);                // 解锁
    return true;
}


JNIEXPORT jstring JNICALL
Java_com_dji_uxsdkdemo_VideoDataActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jdoubleArray JNICALL
Java_com_dji_uxsdkdemo_VideoDataActivity_exeAruco(JNIEnv *env, jobject thiz, jobject input,
                                                  jint width, jint height) {
    // TODO: implement exeAruco()
//    jbyte *y_bytes = env->GetByteArrayElements(input, NULL);
//    unsigned char *in = (unsigned char *) y_bytes;
//    Mat image(height, width, CV_8UC4, (void *) in);
//    Mat newImg;
//    image.copyTo(newImg);

    //std::vector<double> cameraMatrix={1317.45417, 0.0, 835.017019, 0.0, 1355.34823, 538.103069,0.0, 0.0, 1.0};
    //std::vector<double> coff={-0.1077157,0.5860667,-0.00435608,0.00724647,-1.44876692};
    cv::Mat cameraMatrix(
            cv::Matx33f(1317.45417, 0.0, 835.017019, 0.0, 1355.34823, 538.103069, 0.0, 0.0, 1.0));
    Mat outputImage, cMatrix, dcoff;
    bool ret = BitmapToMatrix(env, input, outputImage);          // Bitmap 转 cv::Mat
    if (ret == false) {
        return 0;
    }

    if (outputImage.type() == CV_8UC4)
        cvtColor(outputImage, outputImage, COLOR_RGB2GRAY);

    std::vector<int> markerIds;
    std::vector<std::vector<cv::Point2f>> markerCorners, rejectedCandidates;
    cv::Ptr<cv::aruco::DetectorParameters> parameters = cv::aruco::DetectorParameters::create();
    parameters->adaptiveThreshWinSizeMax = 33;
    parameters->adaptiveThreshWinSizeMin = 13;
    parameters->adaptiveThreshWinSizeStep = 10;
    parameters->minMarkerPerimeterRate = 0.06;
    parameters->polygonalApproxAccuracyRate = 0.06;

    cv::Ptr<cv::aruco::Dictionary> dictionary = cv::aruco::getPredefinedDictionary(
            cv::aruco::DICT_6X6_1000);
    cv::aruco::detectMarkers(outputImage, dictionary, markerCorners, markerIds, parameters,
                             rejectedCandidates);
    //__android_log_print(ANDROID_LOG_DEBUG,"opencv","detect over");
    jdouble *jdouble1;

    std::vector<cv::Vec3d> rvecs, tvecs;

    if (!markerIds.empty()) {


//        cMatrix = Mat(cameraMatrix);
//        cMatrix = cMatrix.reshape(1,3);
//        dcoff = Mat(coff);
//        dcoff = dcoff.reshape(1,1);
        cv::aruco::estimatePoseSingleMarkers(markerCorners, 0.15, cameraMatrix, noArray(), rvecs,
                                             tvecs);
        //res = new jint[rvecs.size()*3];
        //res = new jint[tvecs.size()*3];
        if (!tvecs.empty()) {
            jdouble1 = new jdouble[2];
            jdouble1[0] = 0;
            jdouble1[1] = 0;
            for (int i = 0; i < markerCorners[0].size(); ++i) {
                jdouble1[0] += markerCorners[0][i].x;
                jdouble1[1] += markerCorners[0][i].y;
            }

            jdouble1[0] = jdouble1[0] / 4;
            jdouble1[1] = jdouble1[1] / 4;

            jdouble1[0] = jdouble1[0] - (outputImage.cols / 2);
            jdouble1[1] = jdouble1[1] - (outputImage.rows / 2);

//            double r11 = rvecs[0][0];
//            double r21 = rvecs[1][0];
//            double r31 = rvecs[2][0];
//            double r32 = rvecs[2][1];
//            double r33 = rvecs[2][2];
//
//            double thetaz = atan2(r21,r11)/CV_PI *180;
//            double thetay = atan2(-1*r31, sqrt(r32*r32+r33*r33)/CV_PI*180);
//            double thetax = atan2(r32,r33)/CV_PI *180;
//
//            double tx = tvecs[0][0];
//            double ty = tvecs[0][1];
//            double tz = tvecs[0][2];
//
//            double x = tx, y = ty, z = tz;
//
//            codeRotateByZ(x, y, -1 * thetaz, x, y);
//            codeRotateByY(x, z, -1 * thetay, x, z);
//            codeRotateByX(y, z, -1 * thetax, y, z);
//
////        jdouble1[0] = 1;
//            jdouble1[0] = x*-1;
//            jdouble1[1] = y*-1;
//            jdouble1[2] = z*-1;
        } else {
            jdouble1 = new jdouble[1];
            jdouble1[0] = -2.0;
        }
        //__android_log_print(ANDROID_LOG_DEBUG,"opencv","estimate over");
    } else {
        jdouble1 = new jdouble[1];
        jdouble1[0] = -1.0;
    }

    int len, tlen;
    len = 2;
    if (len == 0)
        len = 1;
    else {
        tlen = tvecs.size();
        if (tlen == 0)
            len = 1;
        else
            len = len;
    }

    jdoubleArray resd = env->NewDoubleArray(len);
    env->SetDoubleArrayRegion(resd, 0, len, jdouble1);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();//清除引发的异常，在Java层不会打印异常堆栈信息，如果不清除，后面的调用ThrowNew抛出的异常堆栈信息会
//覆盖前面的异常信息
        jclass cls_exception = env->FindClass("java/lang/Exception");
        env->ThrowNew(cls_exception, "call java static method ndk error");
    }

    delete[] jdouble1;
    return resd;
}

JNIEXPORT jfloatArray JNICALL
Java_com_dji_uxsdkdemo_MainActivity_calibratePoint(JNIEnv *env, jobject thiz, jobject input,
                                                   jint width, jint height) {
    // TODO: implement calibratePoint()
    Mat outputImage;
    bool ret = BitmapToMatrix(env, input, outputImage);          // Bitmap 转 cv::Mat
    if (ret == false) {
        return 0;
    }

    if (outputImage.type() == CV_8UC4)
        cvtColor(outputImage, outputImage, COLOR_RGB2GRAY);

    std::vector<int> markerIds;
    std::vector<std::vector<cv::Point2f>> markerCorners, rejectedCandidates;
    cv::Ptr<cv::aruco::DetectorParameters> parameters = cv::aruco::DetectorParameters::create();
    parameters->adaptiveThreshWinSizeMax = 33;
    parameters->adaptiveThreshWinSizeMin = 13;
    parameters->adaptiveThreshWinSizeStep = 10;
    parameters->minMarkerPerimeterRate = 0.06;
    parameters->polygonalApproxAccuracyRate = 0.06;

    cv::Ptr<cv::aruco::Dictionary> dictionary = cv::aruco::getPredefinedDictionary(
            cv::aruco::DICT_6X6_1000);
    cv::aruco::detectMarkers(outputImage, dictionary, markerCorners, markerIds, parameters,
                             rejectedCandidates);
    jfloat *jfloat1;

    if (!markerIds.empty()) {
        jfloat1 = new jfloat[2];
        jfloat1[0] = 0;
        jfloat1[1] = 0;
        for (int i = 0; i < markerCorners[0].size(); ++i) {
            jfloat1[0] += markerCorners[0][i].x;
            jfloat1[1] += markerCorners[0][i].y;
        }

        jfloat1[0] = jfloat1[0] / 4;
        jfloat1[1] = jfloat1[1] / 4;

        jfloat1[0] = jfloat1[0] - (outputImage.cols / 2);
        jfloat1[1] = jfloat1[1] - (outputImage.rows / 2);

    } else {
        jfloat1 = new jfloat[1];
        jfloat1[0] = 5000;
    }

    int len, tlen;
    len = 2;
    if (len == 0)
        len = 1;
    else {
        len = len;
    }

    jfloatArray resd = env->NewFloatArray(len);
    env->SetFloatArrayRegion(resd, 0, len, jfloat1);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();//清除引发的异常，在Java层不会打印异常堆栈信息，如果不清除，后面的调用ThrowNew抛出的异常堆栈信息会
//覆盖前面的异常信息
        jclass cls_exception = env->FindClass("java/lang/Exception");
        env->ThrowNew(cls_exception, "call java static method ndk error");
    }

    delete[] jfloat1;
    return resd;
}

JNIEXPORT void JNICALL
Java_com_dji_uxsdkdemo_MainActivity_cameraCalibrate(JNIEnv *env, jobject thiz, jobjectArray bmps,
                                                    jlong rvecs, jlong tvecs) {
    // TODO: implement cameraCalibrate()
    Mat *outputImage = new Mat[10];
    for (int i = 0; i < 10; ++i) {
        BitmapToMatrix(env, bmps + i, *(outputImage + i));
        cvtColor(outputImage[i], outputImage[i], COLOR_RGB2GRAY);
    }
    //TODO: biaodingCode

}

}

