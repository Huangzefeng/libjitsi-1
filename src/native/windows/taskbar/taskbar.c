/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "taskbar.h"

#ifdef INITGUID
    #include <mmdeviceapi.h>
#else /* #ifdef INITGUID */
    #define INITGUID
    #include <mmdeviceapi.h>
    #undef INITGUID
#endif /* #ifdef INITGUID */

#include <audioclient.h> /* IAudioClient */
#include <mmreg.h> /* WAVEFORMATEX */
#include <objbase.h>
#include <stdint.h> /* intptr_t */
#include <string.h>
#include <shobjidl.h>
#include <propkey.h>
#include <propvarutil.h>
#include <shlobj.h>
#include <shellapi.h>
#include <windows.h> /* LoadLibrary, GetProcAddress */
#include <jni.h>

#include "HResultException.h"
#include "Typecasting.h"

HINSTANCE hInst;

BOOL APIENTRY DllMain( HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpvReserverd)
{	 
	switch (ul_reason_for_call) {
		case DLL_PROCESS_ATTACH:
			hInst = (HINSTANCE)hModule;
	}
	return TRUE;
}

ITaskbarList3* getTaskBar()
{
	ITaskbarList3 *taskBar;
	CoCreateInstance(__uuidof(CLSID_TaskbarList), NULL, CLSCTX_ALL, __uuidof(IID_ITaskbarList3), (void**)&taskBar);
	return taskBar;
}

JNIEXPORT jint JNICALL Java_net_java_sip_communicator_service_taskbar_TaskbarIconOverlay_SetOverlayIcon
	(JNIEnv *env, jclass cls, jint iconid, jstring title)
{
	ITaskbarList3 *taskBar = getTaskBar();

	const char *str = (*env)->GetStringUTFChars(env, title, 0);
	HWND hwnd = FindWindow(NULL, str);
	(*env)->ReleaseStringUTFChars(env, title, str);

	if (taskBar != NULL)
	{
		HRESULT hr;
		HICON hIcon = NULL;

		// Load the requested icon
		hIcon = LoadIcon(hInst, MAKEINTRESOURCE(iconid));

		// Set the window's overlay icon
		hr = ITaskbarList3_SetOverlayIcon(taskBar, hwnd, hIcon, NULL);

		if (hIcon) 
		{
			// Need to clean up the icon as we no longer need it
			DestroyIcon(hIcon);
		}

		return hr;
	}
	return -1;
}