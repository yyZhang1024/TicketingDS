package ticketingsystem;


class SiteState implements SiteStateBase{
    private int SiteRouteId;   // 车次
    private int SiteCoachId;   // 车厢号
    private int SiteSeatId;    // 座位号
    private int siteStateBits;
    @Override
    public String toString(){
        String pnt = " SiteState   SiteRouteId: " + SiteRouteId + " SiteCoachId: " + SiteCoachId + " SiteSeatId: " + SiteSeatId + "\n";
        return pnt;
    }

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
        siteStateBits = 0;
    }
    int getIndex(Ticket ticket) {
        int departure = ticket.departure - 1;
        int arrival = ticket.arrival - 1;
        return (departure*(57 - departure) / 2) + arrival - 1;
    }
    int getIndex(int departure, int arrival) {
        departure--;
        arrival--;
        return (departure*(57 - departure) / 2) + arrival - 1;
    }
    public void AddPassenger(Ticket ticket) {
        // int departure = ticket.departure;
        // int arrival = ticket.arrival;
        // departure--;
        // arrival--;
        // int base = 2 << departure;
        // for (int i = departure; i < arrival; i++) {
        //     siteStateBits |= base;
        //     base <<= 1;
        // }
       final int bits = allSateArray[getIndex(ticket)];
       siteStateBits |= bits;
    }

    public void RemovePassenger(Ticket ticket) {
        // int occupyBits = 0;
        // int departure = ticket.departure;
        // int arrival = ticket.arrival;
        // departure--;
        // arrival--;
        // int base = 2 << departure;
        // for (int i = departure; i < arrival; i++) {
        //     occupyBits |= base;
        //     base <<= 1;
        // }
        // occupyBits = ~occupyBits;
        // siteStateBits &= occupyBits;
       int bits = allSateArray[getIndex(ticket)];
       bits = ~bits;
       siteStateBits &= bits;
    }
    public boolean haveSite(int departure, int arrival) {
        // departure--;
        // arrival--;

        // int occupyBits = 0;
        // int base = 2 << departure;
        // for (int i = departure; i < arrival; i++) {
        //     occupyBits |= base;
        //     base <<= 1;
        // }
        // return (occupyBits & siteStateBits) == 0;
       final int bits = allSateArray[getIndex(departure, arrival)];
       return (bits & siteStateBits) == 0;
    }
    public void printSite() {
        System.out.print("Site Bits : ");
        for (int i = 0; i < 31; i++) {
            int base = 2 << i;
            System.out.print((base & siteStateBits) == 0 ? 0 : 1);
        }
        System.out.println();
    }
}