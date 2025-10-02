package com.cchat.cclient.services;

import com.cchat.cclient.model.MessageDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class BubblePrinter {
    
    private static final String TL = "┌";
    private static final String TR = "┐";
    private static final String BL = "└";
    private static final String BR = "┘";
    private static final String VL = "│";
    private static final String HL = "─";

    private static final ZoneId UI_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm").withZone(UI_ZONE);

    public int width() {
        try {
            String cols = System.getenv("COLUMNS");
            if (cols != null) return Math.max(40, Integer.parseInt(cols));
        } catch (Exception ignored) {}
        return 80;
    }
    
    public List<String> wrap(String s, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) { out.add(""); return out; }

        String[] words = s.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            if (line.length() == 0) {
                if (w.length() <= maxWidth) line.append(w);
                else {
                    int i = 0;
                    while (i < w.length()) {
                        int end = Math.min(i + maxWidth, w.length());
                        out.add(w.substring(i, end));
                        i = end;
                    }
                }
            } else if (line.length() + 1 + w.length() <= maxWidth) {
                line.append(' ').append(w);
            } else {
                out.add(line.toString());
                line.setLength(0);
                if (w.length() <= maxWidth) line.append(w);
                else {
                    int i = 0;
                    while (i < w.length()) {
                        int end = Math.min(i + maxWidth, w.length());
                        out.add(w.substring(i, end));
                        i = end;
                    }
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
    
    public void print(MessageDto m, long selfId) {
        boolean mine = (m.getSender_id() != null && m.getSender_id().equals(selfId));
        int w = width();

        int maxBubble = Math.min(60, Math.max(30, w - 10));
        int innerWidth = maxBubble;
        int boxWidth = innerWidth + 4;

        List<String> lines = wrap(safe(m.getBody()), innerWidth);

        String pad = " ".repeat(mine ? Math.max(0, w - (boxWidth + 4)) : 2);

        String who = mine ? "me" : String.valueOf(m.getSender_id());
        String time = (m.getCreatedAt() != null) ? HHMM.format(m.getCreatedAt()) : HHMM.format(Instant.now());
        String header = (time == null) ? who : (who + " [" + time + "]");

        System.out.println();

        if (mine) {
            int gap = Math.max(0, boxWidth - header.length());
            System.out.println(pad + " ".repeat(gap) + header);
        } else {
            System.out.println(pad + header);
        }

        System.out.println(pad + TL + repeat(HL, innerWidth + 2) + TR);

        for (String line : lines) {
            String padded = line + repeat(" ", innerWidth - line.length());
            System.out.println(pad + VL + " " + padded + " " + VL);
        }

        System.out.println(pad + BL + repeat(HL, innerWidth + 2) + BR);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String repeat(String s, int n) {
        if (n <= 0)
            return "";
        return s.repeat(n);
    }
}