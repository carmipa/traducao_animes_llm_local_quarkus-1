package org.traducao.projeto.remuxer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remuxer")
public class RemuxerProperties {
    private String mkvmergePath;

    public RemuxerProperties() {
    }

    public RemuxerProperties(String mkvmergePath) {
        this.mkvmergePath = mkvmergePath;
    }

    public String mkvmergePath() { return mkvmergePath; }
    public String getMkvmergePath() { return mkvmergePath; }
    public void setMkvmergePath(String mkvmergePath) { this.mkvmergePath = mkvmergePath; }

    public String resolverMkvmergePath() {
        if (mkvmergePath == null || mkvmergePath.isBlank()) {
            return "mkvmerge"; // Tenta usar do PATH
        }
        return mkvmergePath;
    }
}
