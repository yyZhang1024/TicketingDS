package ticketingsystem;

import java.util.LinkedList;

public interface SiteStateBase {
    //boolean valid;
    int SiteRouteId = 0;   // 车次
    int SiteCoachId = 0;   // 车厢号
    int SiteSeatId = 0;    // 座位号
    //    boolean haveOnes;   // 是否被占用
    LinkedList<Ticket> passengerTickets = null;
    @Override
    String toString();
    String toAllString();
    int GetCoach();
    int GetSeat();
    boolean isNonePassenger();
    boolean AddPassenger(Ticket ticket);
    boolean RemovePassenger(Ticket ticket);
    boolean IsCross(int departure1, int departure2, int arrival1, int arrival2);
    public boolean haveSite(int departure, int arrival);
}
