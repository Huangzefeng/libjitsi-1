/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI */

#ifndef _Included_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
#define _Included_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CloseHandle
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CloseHandle
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CoCreateGuid
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CoCreateGuid
  (JNIEnv *, jclass);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CoCreateInstance
 * Signature: (Ljava/lang/String;JILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CoCreateInstance
  (JNIEnv *, jclass, jstring, jlong, jint, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CoInitializeEx
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CoInitializeEx
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CoTaskMemFree
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CoTaskMemFree
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CoUninitialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CoUninitialize
  (JNIEnv *, jclass);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    CreateEvent
 * Signature: (JZZLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_CreateEvent
  (JNIEnv *, jclass, jlong, jboolean, jboolean, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioCaptureClient_GetNextPacketSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioCaptureClient_1GetNextPacketSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioCaptureClient_Read
 * Signature: (J[BIIIIII)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioCaptureClient_1Read
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint, jint, jint, jint, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioCaptureClient_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioCaptureClient_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_GetBufferSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1GetBufferSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_GetCurrentPadding
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1GetCurrentPadding
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_GetDefaultDevicePeriod
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1GetDefaultDevicePeriod
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_GetMinimumDevicePeriod
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1GetMinimumDevicePeriod
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_GetService
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1GetService
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_Initialize
 * Signature: (JIIJJJLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1Initialize
  (JNIEnv *, jclass, jlong, jint, jint, jlong, jlong, jlong, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_IsFormatSupported
 * Signature: (JIJ)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1IsFormatSupported
  (JNIEnv *, jclass, jlong, jint, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_SetEventHandle
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1SetEventHandle
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_Start
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1Start
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioClient_Stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioClient_1Stop
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioRenderClient_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioRenderClient_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IAudioRenderClient_Write
 * Signature: (J[BIIIIII)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IAudioRenderClient_1Write
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint, jint, jint, jint, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_Activate
 * Signature: (JLjava/lang/String;IJ)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1Activate
  (JNIEnv *, jclass, jlong, jstring, jint, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_GetId
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1GetId
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_GetState
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1GetState
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_OpenPropertyStore
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1OpenPropertyStore
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_QueryInterface
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1QueryInterface
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDevice_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDevice_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceCollection_GetCount
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceCollection_1GetCount
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceCollection_Item
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceCollection_1Item
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceCollection_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceCollection_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceEnumerator_EnumAudioEndpoints
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceEnumerator_1EnumAudioEndpoints
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceEnumerator_GetDevice
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceEnumerator_1GetDevice
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMDeviceEnumerator_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMDeviceEnumerator_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMEndpoint_GetDataFlow
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMEndpoint_1GetDataFlow
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IMMEndpoint_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IMMEndpoint_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IPropertyStore_GetString
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IPropertyStore_1GetString
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    IPropertyStore_Release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_IPropertyStore_1Release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    PSPropertyKeyFromString
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_PSPropertyKeyFromString
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    ResetEvent
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_ResetEvent
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WaitForSingleObject
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WaitForSingleObject
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_alloc
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1alloc
  (JNIEnv *, jclass);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_fill
 * Signature: (JCCIICCC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1fill
  (JNIEnv *, jclass, jlong, jchar, jchar, jint, jint, jchar, jchar, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getCbSize
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getCbSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getNAvgBytesPerSec
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getNAvgBytesPerSec
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getNBlockAlign
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getNBlockAlign
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getNChannels
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getNChannels
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getNSamplesPerSec
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getNSamplesPerSec
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getWBitsPerSample
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getWBitsPerSample
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_getWFormatTag
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1getWFormatTag
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setCbSize
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setCbSize
  (JNIEnv *, jclass, jlong, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setNAvgBytesPerSec
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setNAvgBytesPerSec
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setNBlockAlign
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setNBlockAlign
  (JNIEnv *, jclass, jlong, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setNChannels
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setNChannels
  (JNIEnv *, jclass, jlong, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setNSamplesPerSec
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setNSamplesPerSec
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setWBitsPerSample
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setWBitsPerSample
  (JNIEnv *, jclass, jlong, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_setWFormatTag
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1setWFormatTag
  (JNIEnv *, jclass, jlong, jchar);

/*
 * Class:     org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI
 * Method:    WAVEFORMATEX_sizeof
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jitsi_impl_neomedia_jmfext_media_protocol_wasapi_WASAPI_WAVEFORMATEX_1sizeof
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
