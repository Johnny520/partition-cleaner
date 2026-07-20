package com.example.partitioncleaner;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 资料页（头像 / 昵称 / 个性签名）持久化。
 * 默认值：头像 avatar_01、昵称「孤独的根号3」、签名「这个人很懒，除了帅其它什么也没留下」。
 */
public class ProfilePrefs {
    private static final String NAME = "profile_prefs";
    private static final String K_AVATAR = "avatar";
    private static final String K_NAME = "name";
    private static final String K_SIGN = "sign";

    public static final String DEFAULT_AVATAR = "avatar_01";
    public static final String DEFAULT_NAME = "孤独的根号3";
    public static final String DEFAULT_SIGN = "这个人很懒，除了帅其它什么也没留下";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getAvatar(Context c) {
        return sp(c).getString(K_AVATAR, DEFAULT_AVATAR);
    }

    public static void setAvatar(Context c, String avatarName) {
        sp(c).edit().putString(K_AVATAR, avatarName).apply();
    }

    public static String getName(Context c) {
        return sp(c).getString(K_NAME, DEFAULT_NAME);
    }

    public static void setName(Context c, String name) {
        sp(c).edit().putString(K_NAME, name == null ? "" : name.trim()).apply();
    }

    public static String getSign(Context c) {
        return sp(c).getString(K_SIGN, DEFAULT_SIGN);
    }

    public static void setSign(Context c, String sign) {
        sp(c).edit().putString(K_SIGN, sign == null ? "" : sign.trim()).apply();
    }

    /** 根据头像资源名解析 drawable id，解析失败回退默认头像。 */
    public static int getAvatarResId(Context c) {
        String n = getAvatar(c);
        int id = c.getResources().getIdentifier(n, "drawable", c.getPackageName());
        if (id == 0) {
            id = c.getResources().getIdentifier(DEFAULT_AVATAR, "drawable", c.getPackageName());
        }
        return id;
    }

    /** 可选头像资源名列表（与 res/drawable/avatar_01..08.png 对应）。 */
    public static String[] AVATARS = {
            "avatar_01", "avatar_02", "avatar_03", "avatar_04",
            "avatar_05", "avatar_06", "avatar_07", "avatar_08"
    };
}
