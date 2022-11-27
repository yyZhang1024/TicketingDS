package ticketingsystem;

import java.util.LinkedList;

class SiteState implements SiteStateBase{
    //boolean valid;
    private volatile int  SiteRouteId;   // 车次
    private volatile int SiteCoachId;   // 车厢号
    private volatile int SiteSeatId;    // 座位号
    //    boolean haveOnes;   // 是否被占用
    private LinkedList<Ticket> passengerTickets;
    @Override
    public String toString(){
        String pnt = " SiteState   SiteRouteId: " + SiteRouteId + " SiteCoachId: " + SiteCoachId + " SiteSeatId: " + SiteSeatId + "\n";
        return pnt;
    }
    public String toAllString(){
        // String pnt = " SiteState   SiteRouteId: " + SiteRouteId + " SiteCoachId: " + SiteCoachId + " SiteSeatId: " + SiteSeatId;
        String pnt = "\n";
        for (Ticket ticket: passengerTickets) {
            pnt += ticket.passenger +  " route: " + ticket.route +  " tid: " + ticket.tid +  " departure: " + ticket.departure +  " arrival:" + ticket.arrival +  "\n";
        }
        return pnt;
    }
    //    public int GetRoute() {
    //        return SiteRouteId;
    //    }
    public int GetCoach() {
        return SiteCoachId;
    }
    public int GetSeat() {
        return SiteSeatId;
    }
    public SiteState(int routeid, int coachid, int seatid) {
        SiteRouteId = routeid;
        SiteCoachId = coachid;
        SiteSeatId = seatid;
        // haveOnes = false;
        passengerTickets = new LinkedList();
        // valid = true;
    }
    public boolean isNonePassenger(){
        return passengerTickets.isEmpty();
    }
    public boolean AddPassenger(Ticket ticket) {
        // return passengerTickets.add(ticket);
        boolean t = passengerTickets.add(ticket);
        assert (t);
        return true;
    }

    public boolean RemovePassenger(Ticket ticket) {
        // return passengerTickets.remove(ticket);
        boolean t = passengerTickets.remove(ticket);
        assert (t);
        return true;
    }
    public boolean IsCross(int departure1, int departure2, int arrival1, int arrival2)
    {
        // int departure = Math.max(departure1, departure2);
        // int arrival = Math.min(arrival1, arrival2);
        // return arrival >= departure;
        // return !((arrival1 <= departure2) || (departure1 >= arrival2));
        return arrival2 >= departure1 && arrival2 <= arrival1 || departure2 >= departure1 && departure2 <= arrival1 || departure1 >= departure2 && departure1 <= arrival2 || arrival1 >= departure2 && arrival1 <= arrival2;
    }
    public boolean haveSite(int departure, int arrival) {
        for(Ticket ticket : passengerTickets){
            if (IsCross(departure, ticket.departure, arrival - 1, ticket.arrival - 1)) {
                // if (IsCross(departure, ticket.departure, arrival, ticket.arrival)) {
                return false;
            }
        }
        return true;
    }
}