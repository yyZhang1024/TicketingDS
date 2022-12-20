package ticketingsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;


public class TicketingDS implements TicketingSystem {
    private final int routenum;      // 车次 数目
    private final int coachnum;      // 每次列车的车厢数目
    private final int seatnum;       // 每节车厢的座位 数目
    private final int stationnum;    // 每个车次经停站的数量
    private final int threadnum;     // 线程数目


    // (车次号 - 1) * 车厢数目 * 车厢座位数目 + (车厢号 - 1) * 车厢座位数目 + 座位号 ====> 线性座位id (如果从1开始计算)
    // 车次 * 车厢数目 + 车厢号
    private ArrayList<SiteState> allSitesState;
    //    private final BackOffAtomicLong nextTid;
    private final AtomicLong nextTid;
    private int getLinearIdFromZero(int route, int coach, int seat) {
        int linearId = route * coachnum * seatnum + coach  * seatnum + seat;
        return linearId;
    }
    private int getLinearIdFromOne(int route, int coach, int seat) {
        return getLinearIdFromZero(route - 1, coach - 1, seat - 1);
    }
    private int getRouteLastIndex(int route){
        return (route + 1) * coachnum * seatnum - 1;
    }
    private int getRouteFirstIndex(int route){
        return route * coachnum * seatnum;
    }

    boolean illegal(Ticket ticket) {
        if (ticket.arrival > stationnum || ticket.arrival <= ticket.departure || ticket.departure < 1) {
            return true;
        }
        return false;
    }
    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum = _routenum;
        coachnum = _coachnum;
        seatnum = _seatnum;
        stationnum = _stationnum;
        threadnum = _threadnum;
        nextTid = new AtomicLong();
        allSitesState = new ArrayList<>(_routenum*_coachnum*_seatnum);
        // initial Data Structures
        int k = 0;
        for (int _routenumT = 0; _routenumT < _routenum; _routenumT++) {
            for (int _coachnumT = 0; _coachnumT < _coachnum; _coachnumT++){
                for (int _seatnumT = 0; _seatnumT < _seatnum; _seatnumT++) {
                    SiteState temp = new SiteState(_routenumT + 1, _coachnumT + 1, _seatnumT + 1);
                    allSitesState.add(temp);
                    long linearid =  getLinearIdFromZero(_routenumT, _coachnumT, _seatnumT);
                    // if (k != linearid){
                    //     System.out.println("ERROR LINEAR ID!!!");
                    // }
                    k++;
                }
            }
        }
    }


    @Override
    public  Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Ticket ticket = null;
        boolean find = false;
        ticket = new Ticket();
        ticket.tid = nextTid.getAndIncrement();
        ticket.passenger = passenger;
        ticket.route = route;
        ticket.departure = departure;
        ticket.arrival = arrival;

        for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++) {
            SiteState newSite = allSitesState.get(i);
            synchronized (newSite){
                if (newSite.haveSite(departure, arrival)) {
                    //                newSite.printSite();
                    ticket.coach = newSite.GetCoach();
                    ticket.seat = newSite.GetSeat();
                    //                if (!newSite.haveSite(departure, arrival)){
                    //                    continue;
                    //                }
                    newSite.AddPassenger(ticket);
                    // tids.add(ticket.tid);
                    find = true;
                    break;
                }
            } // synchronized
        }

        if (find) {
            return ticket;
        } else {
            return null;
        }
    }

    @Override
    public  int inquiry(int route, int departure, int arrival) {
        int num = 0;
        for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
            // System.out.print("inquiry Site: " + i + ": SitesState: " + allSitesState.get(i).haveSite(departure, arrival) + " Bits :");
            // allSitesState.get(i).printSite();
            if (allSitesState.get(i).haveSite(departure, arrival)) {
                num++;
            }
        }
        return num;
    }

    @Override
    public  boolean refundTicket(Ticket ticket) {
        boolean flag = false;
        int route = ticket.route;
        if (illegal(ticket)){
            // flag = false;
        } else {
            flag = true;
            int i = getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat);
            SiteState siteState = allSitesState.get(i);
            synchronized(siteState) {
                siteState.RemovePassenger(ticket);
            }
        }
        return flag;
    }

    @Override
    public boolean buyTicketReplay(Ticket ticket) {
        return false;
    }

    @Override
    public boolean refundTicketReplay(Ticket ticket) {
        return false;
    }
}

