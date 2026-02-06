package org.taillogs.taillogs.utils;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(?<ERROR>ERROR)|(?<WARN>WARN)|(?<INFO>INFO)"
    );

    public static void applyLogLevelHighlighting(CodeArea logArea) {
        String text = logArea.getText();
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        Matcher matcher = LOG_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);

            if (matcher.group("ERROR") != null) {
                spansBuilder.add(Collections.singleton("error"), matcher.end() - matcher.start());
            } else if (matcher.group("WARN") != null) {
                spansBuilder.add(Collections.singleton("warn"), matcher.end() - matcher.start());
            } else if (matcher.group("INFO") != null) {
                spansBuilder.add(Collections.singleton("info"), matcher.end() - matcher.start());
            }

            lastEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        StyleSpans<Collection<String>> spans = spansBuilder.create();
        logArea.setStyleSpans(0, spans);
    }
}
