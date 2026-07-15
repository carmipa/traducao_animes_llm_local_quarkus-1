package org.traducao.projeto.legendasExtracao.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "extrator")
public class ExtratorProperties {
    private String mkvmergePath;
    private String mkvextractPath;
    private String ffmpegPath;
    private String ffprobePath;

    public ExtratorProperties() {
    }

    public ExtratorProperties(String mkvmergePath, String mkvextractPath, String ffmpegPath, String ffprobePath) {
        this.mkvmergePath = mkvmergePath;
        this.mkvextractPath = mkvextractPath;
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
    }

    public String mkvmergePath() { return mkvmergePath; }
    public String getMkvmergePath() { return mkvmergePath; }
    public void setMkvmergePath(String mkvmergePath) { this.mkvmergePath = mkvmergePath; }

    public String mkvextractPath() { return mkvextractPath; }
    public String getMkvextractPath() { return mkvextractPath; }
    public void setMkvextractPath(String mkvextractPath) { this.mkvextractPath = mkvextractPath; }

    public String ffmpegPath() { return ffmpegPath; }
    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }

    public String ffprobePath() { return ffprobePath; }
    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }

    public String resolverMkvmergePath() {
        if (mkvmergePath == null || mkvmergePath.isBlank()) {
            return "mkvmerge";
        }
        return mkvmergePath;
    }

    public String resolverMkvextractPath() {
        if (mkvextractPath == null || mkvextractPath.isBlank()) {
            return "mkvextract";
        }
        return mkvextractPath;
    }

    public String resolverFfmpegPath() {
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            return "ffmpeg";
        }
        return ffmpegPath;
    }

    public String resolverFfprobePath() {
        if (ffprobePath == null || ffprobePath.isBlank()) {
            return "ffprobe";
        }
        return ffprobePath;
    }
}
