//
// Created by hyx_4 on 2021/6/10.
//
#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>
//#include <android/log.h>
//#include <string>
using namespace cv;
extern "C" {
JNIEXPORT jstring JNICALL
Java_com_dji_uxsdkdemo_VideoDataActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jbyteArray JNICALL
Java_com_dji_uxsdkdemo_VideoDataActivity_exeAruco(JNIEnv *env, jobject thiz, jbyteArray input,
                                                  jint width, jint height) {
    // TODO: implement exeAruco()
    jbyte *y_bytes = env->GetByteArrayElements(input, NULL);
    unsigned char *in = (unsigned char *) y_bytes;
    Mat image(height, width, CV_8UC4, (void *) in);
    Mat newImg;
    image.copyTo(newImg);

    std::vector<int> markerIds;
    std::vector<std::vector<cv::Point2f>> markerCorners, rejectedCandidates;
    Mat outputImage;
    cvtColor(newImg, outputImage, COLOR_RGBA2BGR);

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


    int size = (5, 5);
    double sigma1 = 15.0;
    double sigma2 = 20.0;
    //cv::GaussianBlur(outputImage,outputImage,Size(11,11),sigma1,sigma2);

//        cv::cvtColor(outputImage,outputImage,COLOR_RGB2GRAY);
//        std::string str = std::to_string(markerIds.size());
////__android_log_print(ANDROID_LOG_DEBUG,"opencv","markerIds=%s", str.c_str());
//        if(markerIds.size()>0){
//            cv::aruco::drawDetectedMarkers(outputImage, markerCorners, markerIds);
////            cv::Mat Kfront;
////            std::vector<cv::Vec3d> rvecs, tvecs;
////            cv:aruco::estimatePoseSingleMarkers(markerCorners, 0.1, Kfront, cv::noArray(), rvecs, tvecs);
////            __android_log_print(ANDROID_LOG_DEBUG,"opencv","success");
//        } else{
//            Point point;
//            point.x = outputImage.rows/2;
//            point.y = outputImage.cols/2;
//            putText(outputImage,"ERROR!",point,FONT_HERSHEY_PLAIN,14,Scalar(0,0,255),2);
//        }
    cvtColor(outputImage, outputImage, COLOR_BGR2RGBA);
    jbyteArray array = env->NewByteArray(width * height * 4);
    jbyte *_data = new jbyte[outputImage.total() * 4];
    for (int i = 0; i < outputImage.total() * 4; i++) {
        _data[i] = outputImage.data[i];
    }
    env->SetByteArrayRegion(array, 0, outputImage.total() * 4, _data);
    delete[]_data;

    return array;
}
}
