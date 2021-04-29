import giveaway.GiveawayRegistry;
import startbot.BotStart;
import threads.StatcordThread;
import threads.StopGiveawayByTimer;
import threads.TopGGApiThread;

public class Main {

  public static void main(String[] args) throws Exception {

    BotStart botStart = new BotStart();
    botStart.startBot();
    GiveawayRegistry.getInstance();

    StatcordThread statcordThread = new StatcordThread();
    statcordThread.start();

    TopGGApiThread topGGApiThread = new TopGGApiThread();
    topGGApiThread.start();

    StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer();
    stopGiveawayByTimer.start();
  }

}
