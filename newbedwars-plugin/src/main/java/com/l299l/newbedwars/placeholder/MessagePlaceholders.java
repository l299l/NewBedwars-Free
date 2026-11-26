package com.l299l.newbedwars.placeholder;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessagePlaceholders {
    private static final Pattern TARGETED = Pattern.compile("%(viewer|killer|victim):([^%\\s]+)%");
    private static final Pattern PLAIN = Pattern.compile("%[A-Za-z0-9.\\-]+_[^%\\s]*%");

    private MessagePlaceholders() {
    }

    public static String apply(Player viewer, MessageContext context, String template) {
        if (!PapiText.isEnabled() || template == null || template.indexOf('%') < 0) return template;
        Matcher matcher = TARGETED.matcher(template);
        StringBuilder out = new StringBuilder();
        int done = 0;
        while (matcher.find()) {
            String role = matcher.group(1);
            boolean isViewer = role.equals("viewer");
            if (!isViewer && !context.targets().containsKey(role)) continue;
            out.append(forSubject(viewer, context, template.substring(done, matcher.start())));
            Player target = isViewer ? viewer : context.targets().get(role);
            if (target != null) out.append(PapiText.apply(target, "%" + matcher.group(2) + "%"));
            done = matcher.end();
        }
        return out.append(forSubject(viewer, context, template.substring(done))).toString();
    }

    private static String forSubject(Player viewer, MessageContext context, String text) {
        Player subject = context.aboutViewer() ? viewer : context.subject();
        return subject == null ? PLAIN.matcher(text).replaceAll("") : PapiText.apply(subject, text);
    }
}
