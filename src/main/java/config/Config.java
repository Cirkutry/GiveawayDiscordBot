package config;

public class Config {

  private static final String DEV_BOT_TOKEN = "";
  private static final String PRODUCTION_BOT_TOKEN = "";
  private static final String TOKEN = PRODUCTION_BOT_TOKEN;
  private static final String CONN = "";
  private static final String USER = "";
  private static final String PASS = "";
  private static final String TOP_GG_API_TOKEN = "";
  private static final String BOT_ID = "808277484524011531"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
  private static final String STATCORD = "";

  public static String getTOKEN() {
    return TOKEN;
  }

  public static String getCONN() {
    return CONN;
  }

  public static String getUSER() {
    return USER;
  }

  public static String getPASS() {
    return PASS;
  }

  public static String getTopGgApiToken() {
    return TOP_GG_API_TOKEN;
  }

  public static String getBotId() {
    return BOT_ID;
  }

  public static String getStatcord() {
    return STATCORD;
  }
}