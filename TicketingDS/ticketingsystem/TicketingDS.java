package ticketingsystem;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SiteState {
    //boolean valid;
    int SiteRouteId;   // 车次
    int SiteCoachId;   // 车厢号
    int SiteSeatId;    // 座位号
//    boolean haveOnes;   // 是否被占用
    LinkedList<Ticket> passengerTickets;
    @Override
    public String toString(){
        String pnt = " SiteState   SiteRouteId: " + SiteRouteId + " SiteCoachId: " + SiteCoachId + " SiteSeatId: " + SiteSeatId;
        return pnt;
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
    public void AddPassenger(Ticket ticket) {
        passengerTickets.add(ticket);
    }

    public void RemovePassenger(Ticket ticket) {
        passengerTickets.remove(ticket);
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
//            if (ticket.departure <= departure && ticket.arrival >= arrive) {
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
    private Map<Long, SiteState> getTidSiteState;
    //指定车次的FreeList，索引从0开始
    private ArrayList<LinkedList<SiteState>> FreeList;
    private AtomicInteger nextTid;

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

        for (int routeIndex = 0; routeIndex < _routenum; routeIndex++) {
            FreeList.add(new LinkedList<>());
        }
        // initial Data Structures
        int k = 0;
        for (int _routenumT = 0; _routenumT < _routenum; _routenumT++) {
            for (int _coachnumT = 0; _coachnumT < _coachnum; _coachnumT++){
                for (int _seatnumT = 0; _seatnumT < _seatnum; _seatnumT++) {

                    SiteState temp = new SiteState(_routenumT + 1, _coachnumT + 1, _seatnumT + 1);
                    allSitesState.add(temp);
                    long linearid =  getLinearIdFromZero(_routenumT, _coachnumT, _seatnumT);
                    if (k != linearid){
                        System.out.println("ERROR LINEAR ID!!!");
                    }
                    k++;
                    FreeList.get(_routenumT).add(temp);
                }
            }
        }
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        lock.lock();
//        System.out.println("222222");
        LinkedList<SiteState> FreeRouteList = FreeList.get(route - 1);

        // FreeList需要进一步封装，保存一个变量表示是否还有空位，如果直接访问FreeRouteList需要加锁
        if(!FreeRouteList.isEmpty()) {
//            System.out.println("111111");
            // FIRST出, LAST进
            //FreeRouteList.getFirst();
            SiteState newSite =  FreeRouteList.removeFirst();

            // ??????????????
            // allSitesState.add(getLinearIdFromOne(newSite.SiteRouteId, newSite.SiteCoachId, newSite.SiteSeatId), new SiteState());
            //
            Ticket ticket = new Ticket();
            // nextTid需要一个锁
            ticket.tid = nextTid.getAndIncrement();
            ticket.passenger = passenger;
            ticket.route = newSite.SiteRouteId;
            ticket.coach = newSite.SiteCoachId;
            ticket.seat = newSite.SiteSeatId;
            ticket.departure = departure;
            ticket.arrival = arrival;
            newSite.AddPassenger(ticket);
            // all sites
//            System.out.println("linear id: " + getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat));
//            System.out.println("Ticket: " + ticket.route + " " + ticket.coach + " " + ticket.seat);

            //allSitesState.set(getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat), newSite);

            // map
//            if (getLinearIdFromOne(newSite.SiteRouteId, newSite.SiteCoachId, newSite.SiteSeatId) != getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat)){
//                System.out.println("ERROR2");
//            }
            getTidSiteState.put(ticket.tid, newSite);
            // 分配tid
            tids.put(ticket.tid, true);
            //nextTid++;
//            for (Ticket t: newSite.passengerTickets) {
//                System.out.println("buy00: " + t.departure + " " + t.arrival);
////                System.out.println("buy01: " +departure + " " + arrival);
//            }
            lock.unlock();
            return ticket;

        }else {
            // travel
            // 注意这时候访问FreeList会出错，需要拿到FreeList的锁
            // 因为过程中可能会有人退票，这时候买票方法不会出错，但是退票会重新把座位
            // 加入FreeList，此时可能卖出去票但FreeList不知道，所以同样需要加锁.
            for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
                SiteState newSite =  allSitesState.get(i);
                if (newSite.haveSite(departure, arrival)) {
                    Ticket ticket = new Ticket();
                    // nextTid需要一个锁
                    ticket.tid = nextTid.getAndIncrement();
                    ticket.passenger = passenger;
                    ticket.route = route;
                    ticket.coach = newSite.SiteCoachId;
                    ticket.seat = newSite.SiteSeatId;
                    ticket.departure = departure;
                    ticket.arrival = arrival;
                    newSite.AddPassenger(ticket);
                    getTidSiteState.put(ticket.tid, newSite);
                    //allSitesState.set(getLinearIdFromOne(ticket.route, ticket.coach, ticket.seat), newSite);
                    // 分配tid
                    tids.put(ticket.tid, true);
                    lock.unlock();
                    return ticket;
                }
            }
        }
        lock.unlock();
        // nextTid++;
        return null;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        // require route lock
        lock.lock();
        int num = 0;
        for (int i = getRouteFirstIndex(route - 1); i <= getRouteLastIndex(route - 1); i++){
            // System.out.println(allSitesState.get(i).passengerTickets.isEmpty());
//            for (Ticket t: allSitesState.get(i).passengerTickets) {
//                System.out.println(t.departure + " " + t.arrival);
//                System.out.println(departure + " " + arrival);
//                System.out.println(num);
//            }

            if (allSitesState.get(i).haveSite(departure, arrival)) {
                num++;
            }
        }
        lock.unlock();
        return num;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        lock.lock();
        // 如果tid是无效的，或者已经被回收了，那失败，否则退票
        if (!tids.containsKey(ticket.tid) || !tids.get(ticket.tid) || illegal(ticket)){
            lock.unlock();
            return false;
        }

        // 进行退票，并且需要checkSite是否没其它的人在同样的座位上了，如果没有了就把座位重新加入FreeList里
        tids.put(ticket.tid, false);
        SiteState siteState = getTidSiteState.get(ticket.tid);
        siteState.RemovePassenger(ticket);
        if(siteState.isNonePassenger()){
            FreeList.get(ticket.route - 1).add(siteState);
        }
        lock.unlock();
        return true;
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
