package ticketingsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;


public class TicketingDS implements TicketingSystem {
    private final int routenum;      // 车次 数目
    private final int coachnum;      // 每次列车的车厢数目
    private final int seatnum;       // 每节车厢的座位 数目
    private final int stationnum;    // 每个车次经停站的数量
    private final int threadnum;     // 线程数目
    private Map<Long, Boolean> tids; // 生成这个tid，用于判断票是否有效，注意：不能用来判断是否还有座位和回收，因为tid一个只会被用一次

    // 买票的话有两种情况
    // case 1： 本身就有座位，那直接返回
    // case 2: 如果没座位，那可能乘坐时间是交错的
    // 对于每一个座位
    // site: 每一个座位维护一个list，座位需要的状态有 三个<乘客，出发点，到达点>，
    // FreeList: 每个座位维护状态是当前座位上卖出去票的引用，
    // 如果有座位没有人做，那直接返回，
    // 否则尝试寻找是否可以和其它人同时做一个座位

    // (车次号 - 1) * 车厢数目 * 车厢座位数目 + (车厢号 - 1) * 车厢座位数目 + 座位号 ====> 线性座位id (如果从1开始计算)
    // 车次 * 车厢数目 + 车厢号

    // NOTICE: allSitesState和FreeList里的SiteState是共享的，因为初始化成了引用,
    // 不需要重新调用allSitesState的set方法对值进行设置，利用引用直接设置即可
    private ArrayList<SiteState> allSitesState;
    private ArrayList<StampedLock> allSitesStateLocks;
    private ArrayList<StampedLock> routeLocks;
    // private Map<Long, SiteState> getTidSiteState;
    //指定车次的FreeList，索引从0开始
//    private final AtomicLong nextTid;
    private final BackOffAtomicLong nextTid;
    //    private static CountingNetwork bitonic;
    // NOTICE: 如果从1开始不能用底下这个函数
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
            return false;
        }
        return true;
    }
    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum = _routenum;
        coachnum = _coachnum;
        seatnum = _seatnum;
        stationnum = _stationnum;
        threadnum = _threadnum;
        tids = new ConcurrentHashMap<>();
        //    nextTid = new AtomicLong();
        nextTid = new BackOffAtomicLong();
        allSitesState = new ArrayList<>(_routenum*_coachnum*_seatnum);
        allSitesStateLocks = new ArrayList<>(_routenum*_coachnum*_seatnum);
        // getTidSiteState = new ConcurrentHashMap<>();
        routeLocks = new ArrayList<>(_routenum);
        // bitonic = new BitonicNetwork(_threadnum + 1);
        // initial Data Structures
        int k = 0;
        for (int _routenumT = 0; _routenumT < _routenum; _routenumT++) {
            routeLocks.add(_routenumT, new StampedLock());
            for (int _coachnumT = 0; _coachnumT < _coachnum; _coachnumT++){
                for (int _seatnumT = 0; _seatnumT < _seatnum; _seatnumT++) {
                    SiteState temp = new SiteState(_routenumT + 1, _coachnumT + 1, _seatnumT + 1);
                    allSitesState.add(temp);
                    allSitesStateLocks.add(new StampedLock());
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
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Ticket ticket = null;
        boolean find = false;
        ticket = new Ticket();
        ticket.tid = nextTid.getAndIncrement();
        ticket.passenger = passenger;
        ticket.route = route;
        ticket.departure = departure;
        ticket.arrival = arrival;
        for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
            SiteState newSite =  allSitesState.get(i);
            long stampRead = allSitesStateLocks.get(i).readLock();
            if (newSite.haveSite(departure, arrival)) {
                allSitesStateLocks.get(i).unlockRead(stampRead);
                ticket.coach = newSite.GetCoach();
                ticket.seat = newSite.GetSeat();
                long stampWrite = allSitesStateLocks.get(i).writeLock();
                if (!newSite.haveSite(departure, arrival)){
                    allSitesStateLocks.get(i).unlockWrite(stampWrite);
                    continue;
                }
                newSite.AddPassenger(ticket);
                tids.put(ticket.tid, true);
                allSitesStateLocks.get(i).unlockWrite(stampWrite);
                find = true;
                break;
            }else {
                allSitesStateLocks.get(i).unlockRead(stampRead);
            }
        }
        if (find) {
            return ticket;
        } else {
            return null;
        }
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        // require route lock
        int num = 0;
        for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
            long stampRead = allSitesStateLocks.get(i).readLock();
            if (allSitesState.get(i).haveSite(departure, arrival)) {
                num++;
            }
            allSitesStateLocks.get(i).unlockRead(stampRead);
        }
        return num;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        boolean flag = false;
        int route = ticket.route;
        if (!tids.containsKey(ticket.tid) || !tids.get(ticket.tid) || illegal(ticket)){
            // flag = false;
        } else {
            // 进行退票，并且需要checkSite是否没其它的人在同样的座位上了，如果没有了就把座位重新加入FreeList里
            if (!tids.get(ticket.tid)) {
                // flag = false;
            } else {
                // flag = true;
                int i = getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat);
                SiteState siteState = allSitesState.get(i);
                tids.put(ticket.tid, false);
                long stampWrite = allSitesStateLocks.get(i).writeLock();
                siteState.RemovePassenger(ticket);
                allSitesStateLocks.get(i).unlockWrite(stampWrite);
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

