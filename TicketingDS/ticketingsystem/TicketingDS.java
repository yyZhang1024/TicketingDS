package ticketingsystem;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SiteState {
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
//        haveOnes = false;
        passengerTickets = new LinkedList();
        //valid = true;
    }
    public boolean isNonePassenger(){
        return passengerTickets.isEmpty();
    }
    public boolean AddPassenger(Ticket ticket) {
//        return passengerTickets.add(ticket);
        boolean t = passengerTickets.add(ticket);
        assert (t);
        return true;
    }

    public boolean RemovePassenger(Ticket ticket) {
//        return passengerTickets.remove(ticket);
        boolean t = passengerTickets.remove(ticket);
        assert (t);
        return true;
    }
    private boolean IsCross(int departure1, int departure2, int arrival1, int arrival2)
    {
//        int departure = Math.max(departure1, departure2);
//        int arrival = Math.min(arrival1, arrival2);
//        return arrival >= departure;
//        return !((arrival1 <= departure2) || (departure1 >= arrival2));
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

    public SiteState() {
//        haveOnes = false;
        //valid = false;
    }
}
public class TicketingDS implements TicketingSystem {
    private final int routenum;      // 车次 数目
    private final int coachnum;      // 每次列车的车厢数目
    private final int seatnum;       // 每节车厢的座位 数目
    private final int stationnum;    // 每个车次经停站的数量
    private final int threadnum;     // 线程数目
    private Map<Long, Boolean> tids = new HashMap<>();            // 生成这个tid，用于判断票是否有效，注意：不能用来判断是否还有座位和回收，因为tid一个只会被用一次

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
    private ArrayList<ReentrantReadWriteLock> routeLocks;
    private Map<Long, SiteState> getTidSiteState;
    //指定车次的FreeList，索引从0开始
    private ArrayList<LinkedList<SiteState>> FreeList;
    private final AtomicInteger nextTid;

    // CheckHaveEmptySite()
    Lock lock;

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

    private boolean illegal(Ticket ticket) {
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
        tids = new HashMap<>();
        nextTid = new AtomicInteger();
        lock = new ReentrantLock();
        allSitesState = new ArrayList<>(_routenum*_coachnum*_seatnum);
        FreeList = new ArrayList<>(_routenum);
        getTidSiteState = new HashMap<>();
        routeLocks = new ArrayList<>(_routenum);

        for (int routeIndex = 0; routeIndex < _routenum; routeIndex++) {
            FreeList.add(new LinkedList<>());
        }
        // initial Data Structures
        int k = 0;
        for (int _routenumT = 0; _routenumT < _routenum; _routenumT++) {
            routeLocks.add(_routenumT, new ReentrantReadWriteLock());
            for (int _coachnumT = 0; _coachnumT < _coachnum; _coachnumT++){
                for (int _seatnumT = 0; _seatnumT < _seatnum; _seatnumT++) {
                    SiteState temp = new SiteState(_routenumT + 1, _coachnumT + 1, _seatnumT + 1);
                    allSitesState.add(temp);
                    long linearid =  getLinearIdFromZero(_routenumT, _coachnumT, _seatnumT);
                    // if (k != linearid){
                    //     System.out.println("ERROR LINEAR ID!!!");
                    // }
                    k++;
                    FreeList.get(_routenumT).add(temp);
                }
            }
        }
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        boolean needNext = true;
        Ticket ticket = null;
        while(needNext) {
            //lock.lock();
            routeLocks.get(route - 1).writeLock().lock();
            try {
                LinkedList<SiteState> FreeRouteList = FreeList.get(route - 1);

                // FreeList需要进一步封装，保存一个变量表示是否还有空位，如果直接访问FreeRouteList需要加锁
                if(!FreeRouteList.isEmpty()) {
                    // FIRST出, LAST进
                    //FreeRouteList.getFirst();
                    SiteState newSite =  FreeRouteList.removeFirst();

                    // ??????????????
                    // allSitesState.add(getLinearIdFromOne(newSite.SiteRouteId, newSite.SiteCoachId, newSite.SiteSeatId), new SiteState());
                    //
                    ticket = new Ticket();
                    // nextTid需要一个锁
                    ticket.tid = nextTid.getAndIncrement();
                    ticket.passenger = passenger;
                    ticket.route = route;
                    ticket.coach = newSite.GetCoach();
                    ticket.seat = newSite.GetSeat();
                    ticket.departure = departure;
                    ticket.arrival = arrival;
                    newSite.AddPassenger(ticket);
                    // all sites

                    //allSitesState.set(getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat), newSite);

                    getTidSiteState.put(ticket.tid, newSite);
                    // 分配tid
                    tids.put(ticket.tid, true);
                    //nextTid++;

                }else {
                    // travel
                    // 注意这时候访问FreeList会出错，需要拿到FreeList的锁
                    // 因为过程中可能会有人退票，这时候买票方法不会出错，但是退票会重新把座位
                    // 加入FreeList，此时可能卖出去票但FreeList不知道，所以同样需要加锁.
                    for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
                        SiteState newSite =  allSitesState.get(i);
                        if (newSite.haveSite(departure, arrival)) {
                            ticket = new Ticket();
                            // nextTid需要一个锁
                            ticket.tid = nextTid.getAndIncrement();
                            ticket.passenger = passenger;
                            ticket.route = route;
                            ticket.coach = newSite.GetCoach();
                            ticket.seat = newSite.GetSeat();
                            ticket.departure = departure;
                            ticket.arrival = arrival;
                            newSite.AddPassenger(ticket);
                            getTidSiteState.put(ticket.tid, newSite);
                            //allSitesState.set(getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat), newSite);
                            // 分配tid
                            tids.put(ticket.tid, true);
                            break;
                            // return ticket;
                        }
                    }
                }
                needNext = false;
            }finally {
                //lock.unlock();
                routeLocks.get(route - 1).writeLock().unlock();
            }
        }

        return ticket;
        // nextTid++;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        // require route lock
        boolean needNext = true;
        int num = 0;
        while(needNext) {
            //lock.lock();
            routeLocks.get(route - 1).readLock().lock();
            try {
                for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
                    // System.out.print(allSitesState.get(i).toAllString());
                    if (allSitesState.get(i).haveSite(departure, arrival)) {
                        num++;
                    }
                }
                needNext = false;
            } finally {
                //lock.unlock();
                routeLocks.get(route - 1).readLock().unlock();
            }
        }

        return num;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        boolean needNext = true;
        boolean flag = false;
        int route = ticket.route;
        while (needNext) {
            //lock.lock();
            routeLocks.get(route - 1).writeLock().lock();
            try {
                if (!tids.containsKey(ticket.tid) || !tids.get(ticket.tid) || illegal(ticket)){
                    // flag = false;
                } else {
                    // 进行退票，并且需要checkSite是否没其它的人在同样的座位上了，如果没有了就把座位重新加入FreeList里
                    if (!tids.get(ticket.tid)) {
                        // flag = false;
                    } else {
                        // flag = true;
                        tids.put(ticket.tid, false);
                        SiteState siteState = getTidSiteState.get(ticket.tid);
                        flag = siteState.RemovePassenger(ticket);
                        if(siteState.isNonePassenger()){
                            FreeList.get(ticket.route - 1).add(siteState);
                        }

                    }

                }
                needNext = false;
            } finally {
                //lock.unlock();
                routeLocks.get(route - 1).writeLock().unlock();
            }
        }
        // 如果tid是无效的，或者已经被回收了，那失败，否则退票
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

    //ToDo

}
