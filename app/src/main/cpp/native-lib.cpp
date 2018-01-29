#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/objdetect.hpp>
#include <opencv2/highgui.hpp>
#include <vector>

using namespace std;
using namespace cv;


CascadeClassifier mainClassifier;
CascadeClassifier nestedClassifier;

extern "C"
{
void detectAndDraw( Mat& img, CascadeClassifier* cascade,
                    CascadeClassifier* nestedCascade,
                    double scale, bool tryflip );

void JNICALL Java_net_monatis_apps_ocvstarter_MainActivity_salt(JNIEnv *env, jobject instance,
                                                                jlong matAddrGray,
                                                                jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }
}

void JNICALL Java_net_monatis_apps_ocvstarter_MainActivity_setCascadePaths(JNIEnv *env, jobject instance, jstring jMainCascadeName, jstring jNestedCascadeName)
{
    const char* cstr1 = env->GetStringUTFChars(jMainCascadeName,NULL);
    const char* cstr2 = env->GetStringUTFChars(jNestedCascadeName,NULL);
    std::string mainCascadeName(cstr1);
    std::string nestedCascadeName(cstr2);
    mainClassifier.load(mainCascadeName);
    nestedClassifier.load(nestedCascadeName);
}

void JNICALL Java_net_monatis_apps_ocvstarter_MainActivity_detectEyes(JNIEnv *env, jobject instance, Mat* frame)
{
    Mat& image = *(Mat *) frame;
    detectAndDraw(image, &mainClassifier, &nestedClassifier, 1.2, false);
}

void detectAndDraw( Mat& img, CascadeClassifier* cascade,
                    CascadeClassifier* nestedCascade,
                    double scale, bool tryflip )
{
    double t = 0;
    std::vector<Rect> faces, faces2;
    const static Scalar colors[] =
            {
                    Scalar(255,0,0),
                    Scalar(255,128,0),
                    Scalar(255,255,0),
                    Scalar(0,255,0),
                    Scalar(0,128,255),
                    Scalar(0,255,255),
                    Scalar(0,0,255),
                    Scalar(255,0,255)
            };
    Mat gray, smallImg;

    cvtColor( img, gray, COLOR_RGBA2GRAY );
    double fx = 1 / scale;
    resize( gray, smallImg, Size(), fx, fx, INTER_LINEAR );
    equalizeHist( smallImg, smallImg );

    t = (double)getTickCount();
    cascade->detectMultiScale( smallImg, faces,
                              1.2, 1, 0
                                      //|CASCADE_FIND_BIGGEST_OBJECT
                                      //|CASCADE_DO_ROUGH_SEARCH
                                      |CV_HAAR_SCALE_IMAGE,
                              Size(30, 30) );
    if( tryflip )
    {
        flip(smallImg, smallImg, 1);
        cascade->detectMultiScale( smallImg, faces2,
                                  1.2, 1, 0
                                          //|CASCADE_FIND_BIGGEST_OBJECT
                                          //|CASCADE_DO_ROUGH_SEARCH
                                          |CV_HAAR_SCALE_IMAGE,
                                  Size(30, 30) );
        for( vector<Rect>::const_iterator r = faces2.begin(); r != faces2.end(); ++r )
        {
            faces.push_back(Rect(smallImg.cols - r->x - r->width, r->y, r->width, r->height));
        }
    }
    t = (double)getTickCount() - t;
    //printf( "detection time = %g ms\n", t*1000/getTickFrequency());
    for ( size_t i = 0; i < faces.size(); i++ )
    {
        Rect r = faces[i];
        Mat smallImgROI;
        vector<Rect> nestedObjects;
        Point center;
        Scalar color = colors[i%8];
        int radius;

        double aspect_ratio = (double)r.width/r.height;
        if( 0.75 < aspect_ratio && aspect_ratio < 1.3 )
        {
            center.x = cvRound((r.x + r.width*0.5)*scale);
            center.y = cvRound((r.y + r.height*0.5)*scale);
            radius = cvRound((r.width + r.height)*0.25*scale);
            circle( img, center, radius, color, 3, 8, 0 );
        }
        else
            rectangle( img, cvPoint(cvRound(r.x*scale), cvRound(r.y*scale)),
                       cvPoint(cvRound((r.x + r.width-1)*scale), cvRound((r.y + r.height-1)*scale)),
                       color, 3, 8, 0);
        if( nestedCascade->empty() )
            continue;
        smallImgROI = smallImg( r );
        nestedCascade->detectMultiScale( smallImgROI, nestedObjects,
                                        1.2, 2, 0
                                                //|CASCADE_FIND_BIGGEST_OBJECT
                                                //|CASCADE_DO_ROUGH_SEARCH
                                                //|CASCADE_DO_CANNY_PRUNING
                                                |CV_HAAR_SCALE_IMAGE,
                                        Size(30, 30) );
        for ( size_t j = 0; j < nestedObjects.size(); j++ )
        {
            Rect nr = nestedObjects[j];
            center.x = cvRound((r.x + nr.x + nr.width*0.5)*scale);
            center.y = cvRound((r.y + nr.y + nr.height*0.5)*scale);
            radius = cvRound((nr.width + nr.height)*0.25*scale);
            circle( img, center, radius, color, 3, 8, 0 );
        }
    }
    //imshow( "result", img );
}



}
