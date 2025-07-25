#include "pch.h"
#include <winrt/base.h>

#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Media.Control.h>
#include <winrt/Windows.Storage.Streams.h>
#include <vector>
#include <cstring>

using namespace winrt;
using namespace Windows::Media::Control;
using namespace Windows::Storage::Streams;

static std::string wstring_to_utf8(const std::wstring& wstr) {
    if (wstr.empty()) return std::string();

    int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
    std::string strTo(size_needed, 0);
    WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
    return strTo;
}

static char* stringToCStr(const std::string& str) {
    if (str.empty()) {
        return nullptr;
    }
    char* cstr = new char[str.length() + 1];
    strcpy_s(cstr, str.length() + 1, str.c_str());
    return cstr;
}

static char* hstringToCStr(const winrt::hstring& hstr) {
    if (hstr.empty()) {
        return nullptr;
    }

    std::wstring wstr(hstr.c_str());

    std::string utf8str = wstring_to_utf8(wstr);

    return stringToCStr(utf8str);
}

extern "C" {

    struct MediaInfoC {
        char* title;
        char* artist;
        char* album;
        uint8_t* thumbnailData;
        int thumbnailSize;
        long long positionMs;
        long long durationMs;
        double playbackRate;
        bool isPlaying;
    };

    __declspec(dllexport) MediaInfoC* getMediaInfo() {
        init_apartment();

        MediaInfoC* mediaInfo = new MediaInfoC();
        // 初始化所有指针为 nullptr
        memset(mediaInfo, 0, sizeof(MediaInfoC));

        try {
            // 获取当前会话
            auto sessionManager = GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();
            auto currentSession = sessionManager.GetCurrentSession();

            if (currentSession) {
                // 获取媒体属性
                auto mediaProperties = currentSession.TryGetMediaPropertiesAsync().get();

                // 设置标题
                mediaInfo->title = hstringToCStr(mediaProperties.Title());

                // 设置艺术家
                mediaInfo->artist = hstringToCStr(mediaProperties.Artist());

                // 设置专辑
                mediaInfo->album = hstringToCStr(mediaProperties.AlbumTitle());

                // 获取时间线属性
                auto timeline = currentSession.GetTimelineProperties();

                // 设置位置和持续时间
                mediaInfo->positionMs = timeline.Position().count();
                mediaInfo->durationMs = timeline.EndTime().count();

                // 获取播放信息
                auto playbackInfo = currentSession.GetPlaybackInfo();

                // 设置播放状态
                mediaInfo->isPlaying =
                    (playbackInfo.PlaybackStatus() == GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing);

                // 设置播放速率
                if (playbackInfo.PlaybackRate()) {
                    mediaInfo->playbackRate = playbackInfo.PlaybackRate().Value();
                }
                else {
                    mediaInfo->playbackRate = 1.0;
                }

                // 获取缩略图
                if (mediaProperties.Thumbnail()) {
                    auto thumbnail = mediaProperties.Thumbnail();
                    auto stream = thumbnail.OpenReadAsync().get();

                    // 读取图片数据
                    Buffer buffer(stream.Size());
                    stream.ReadAsync(buffer, stream.Size(), InputStreamOptions::None).get();

                    // 转换为字节数组
                    auto reader = DataReader::FromBuffer(buffer);
                    std::vector<uint8_t> imageData(buffer.Length());
                    reader.ReadBytes(imageData);

                    // 分配内存并复制数据
                    mediaInfo->thumbnailSize = static_cast<int>(imageData.size());
                    if (mediaInfo->thumbnailSize > 0) {
                        mediaInfo->thumbnailData = new uint8_t[mediaInfo->thumbnailSize];
                        memcpy(mediaInfo->thumbnailData, imageData.data(), mediaInfo->thumbnailSize);
                    }
                }
            }
        }
        catch (std::exception& e) {
            std::printf("Error in getMediaInfo: %s\n", e.what());
        }

        return mediaInfo;
    }

    __declspec(dllexport) void freeMediaInfo(MediaInfoC* info) {
        if (info) {
            // 释放字符串
            delete[] info->title;
            delete[] info->artist;
            delete[] info->album;

            // 释放缩略图数据
            delete[] info->thumbnailData;

            // 释放结构体本身
            delete info;
        }
    }

}