package ticketingsystem;


import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class Verify {
    final static Random rand = new Random();
    static int testnum = 100000;
    static boolean needOutput = true;
    static int routenum = 50;
    static int coachnum = 20;
    static int seatnum = 100;
    static int stationnum = 30;
    static int threadnum = 6;
    static int msec = 0;
    static int nsec = 0;
    static int refRatio = 10;
    static int buyRatio = 30;
    static int inqRatio = 60;
    static int totalPc;
    static TicketingDS tds;
    volatile static boolean initLock = false;
    final static List<String> methodList = new ArrayList<String>();
    final static List<Integer> freqList = new ArrayList<Integer>();
    final static List<Ticket> currentTicket = new ArrayList<Ticket>();
    final static List<String> currentRes = new ArrayList<String>();
    final static List<Integer> currentNum = new ArrayList<Integer>();
    static List<String> logs = new LinkedList<>();
    static ConcurrentLinkedDeque<History> logsList = new ConcurrentLinkedDeque<History>();
    static Map<History, ArrayList<History>> crossToHistory = new HashMap<>();
    final static ArrayList<List<Ticket>> soldTicket = new ArrayList<List<Ticket>>();
    public static String getPassengerName() {
        long uid = rand.nextInt(testnum);
        return "passenger" + uid;
    }
    public static void printHistory() {
        ArrayList aHistory = new ArrayList<History>();
        for (var x : logsList) {
            aHistory.add(x);
        }
        Collections.sort(aHistory);
        for (int i = 0; i <aHistory.size(); i++) {
            History h = (History) aHistory.get(i);
            if (h.action.contains("buyTicket")) {
                System.out.println("preTime: " + h.preTime + " postTime: " + h.postTime + " threadID: " + h.threadID + " action: " + h.action + " tid: " + h.tid + " currentNum: " + h.currentNum +  " result: " + h.result);

            } else if (h.action.contains("inquiry")) {
                System.out.println("preTime: " + h.preTime + " postTime: " + h.postTime + " threadID: " + h.threadID + " action: " + h.action + " tid: " + h.tid + " currentNum: " + h.currentNum +  " result: " + h.result);

            } else {
                System.out.println("preTime: " + h.preTime + " postTime: " + h.postTime + " threadID: " + h.threadID + " action: " + h.action + " tid: " + h.tid + " currentNum: " + h.currentNum +  " result: " + h.result);

            }

        }
    }

    public static boolean isCross(History l, History r) {
        if(r.postTime >= l.preTime && r.postTime <= l.postTime|| r.preTime >= l.preTime && r.preTime <= l.postTime || l.preTime >= r.preTime && l.preTime <= r.postTime || l.postTime >= r.preTime && l.postTime <= r.postTime)
        {
            return true;
        } else {
            return false;
        }
    }
    public static void generateCrossHistory(ArrayList<History> aHistory) {
        for (int i = 0; i < aHistory.size(); i++) {
            History h1 = aHistory.get(i);
            ArrayList<History> list = new ArrayList<>();
            for (int j = 0; j < aHistory.size(); j++) {
                History h2 = aHistory.get(j);
                if (isCross(h1, h2)) {
                    list.add(h2);
                }
            }
            crossToHistory.put(h1, list);
        }
    }

    static boolean verifyRes = true;
    // 找到所有相交的历史
    public static void verify() {
        ArrayList aHistory = new ArrayList<History>();
        for (var x : logsList) {
            aHistory.add(x);
        }
        Collections.sort(aHistory);
        generateCrossHistory(aHistory);
        for (int i = 0; i < aHistory.size(); i++) {
            History h1 = (History) aHistory.get(i);
            for (int j = i + 1; j < aHistory.size(); j++) {
                History h2 = (History) aHistory.get(j);
                if (h1.tid == h2.tid &&  h1.tid != 0) {
                    // 先卖再买，必须相交，相交时可以取相交位置为可线性化点，否则不可线性化
                    if (h2.action == "buyTicket" && h1.action == "refundTicket" && !crossToHistory.get(h1).contains(h2)) {
                        verifyRes = false;
                    }
                    // System.out.println(h1.preTime + " " + h1.postTime + " " + h1.threadID + " " + h1.action + " " + h1.tid + " " + h1.result);
                    // System.out.println(h2.preTime + " " + h2.postTime + " " + h2.threadID + " " + h2.action + " " + h2.tid + " " + h2.result);
                }
            }
        }
        System.out.println("Verify :" + verifyRes);
    }
    public static void initialization() {
        tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
        for(int i = 0; i < threadnum; i++){
            List<Ticket> threadTickets = new ArrayList<Ticket>();
            soldTicket.add(threadTickets);
            currentTicket.add(null);
            currentRes.add("");
            currentNum.add(-1);
        }
        //method freq is up to
        methodList.add("refundTicket");
        freqList.add(refRatio);
        methodList.add("buyTicket");
        freqList.add(refRatio+buyRatio);
        methodList.add("inquiry");
        freqList.add(refRatio+buyRatio+inqRatio);
        totalPc = refRatio+buyRatio+inqRatio;
    }
    public static boolean execute(int num){
        int route, departure, arrival;
        Ticket ticket = new Ticket();
        switch(num){
            case 0://refund
                if(soldTicket.get(ThreadId.get()).size() == 0)
                    return false;
                int n = rand.nextInt(soldTicket.get(ThreadId.get()).size());
                ticket = soldTicket.get(ThreadId.get()).remove(n);

                if(ticket == null){
                    return false;
                }
                currentTicket.set(ThreadId.get(), ticket);
                boolean flag = tds.refundTicket(ticket);
                currentRes.set(ThreadId.get(), "true");
                // for verify
                currentNum.set(ThreadId.get(), tds.inquiry(ticket.route, ticket.departure, ticket.arrival));
                return flag;
            case 1://buy
                String passenger = getPassengerName();
                route = rand.nextInt(routenum) + 1;
                departure = rand.nextInt(stationnum - 1) + 1;
                arrival = departure + rand.nextInt(stationnum - departure) + 1;
                ticket = tds.buyTicket(passenger, route, departure, arrival);

                // for verify after buyTicket
                currentNum.set(ThreadId.get(), tds.inquiry(ticket.route, ticket.departure, ticket.arrival));

                if(ticket == null){
                    ticket = new Ticket();
                    ticket.passenger = passenger;
                    ticket.route = route;
                    ticket.departure = departure;
                    ticket.arrival = arrival;
                    ticket.seat = 0;
                    currentTicket.set(ThreadId.get(), ticket);
                    currentRes.set(ThreadId.get(), "false");
                    return true;
                }
                currentTicket.set(ThreadId.get(), ticket);
                currentRes.set(ThreadId.get(), "true");
                soldTicket.get(ThreadId.get()).add(ticket);
                return true;
            case 2:
                ticket.passenger = getPassengerName();
                ticket.route = rand.nextInt(routenum) + 1;
                ticket.departure = rand.nextInt(stationnum - 1) + 1;
                ticket.arrival = ticket.departure + rand.nextInt(stationnum - ticket.departure) + 1; // arrival is always greater than departure
                ticket.seat = tds.inquiry(ticket.route, ticket.departure, ticket.arrival);
                currentNum.set(ThreadId.get(), ticket.seat);
                currentTicket.set(ThreadId.get(), ticket);
                currentRes.set(ThreadId.get(), "true");
                return true;
            default:
                System.out.println("Error in execution.");
                return false;
        }
    }
    public static void addHistory(long preTime, long postTime, String actionName, boolean flag, String res){
        Ticket ticket = currentTicket.get(ThreadId.get());
        History ht = null;
        ht = new History(preTime, postTime,  actionName, ticket.route, ticket.coach, ticket.departure, ticket.arrival, ticket.seat, ticket.tid, ThreadId.get(), ticket.passenger, flag, res, currentNum.get(ThreadId.get()));

        logsList.add(ht);
    }
    // output through
    public static void multiThreadTest() throws InterruptedException {
        // change global variable
        // threadnum = threadnumChange;
        // msec = msecChange;
        // nsec = nsecChange;

        Thread[] threads = new Thread[threadnum];
        // initialization();
        final long startTime = System.nanoTime();
        for (int i = 0; i < threadnum; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    if(ThreadId.get() == 0){
                        initialization();
                        initLock = true;
                    }
                    else{
                        while(!initLock){
                            ;
                        }
                    }
                    for(int k = 0; k < testnum; k++){
                        int sel = rand.nextInt(totalPc);
                        int cnt = 0;

                        for(int j = 0; j < methodList.size(); j++){
                            if(sel >= cnt && sel < cnt + freqList.get(j)){
                                if(msec != 0 || nsec != 0){
                                    try{
                                        Thread.sleep(msec, nsec);
                                    }catch(InterruptedException e){
                                        return;
                                    }
                                }
                                long preTime = System.nanoTime() - startTime;
                                boolean flag = execute(j);
                                long postTime = System.nanoTime() - startTime;
                                if(flag) {
                                    String res = currentRes.get(ThreadId.get());
                                    addHistory(preTime, postTime, methodList.get(j), flag, res);
                                }
//								if(flag){
//									print(preTime, postTime, methodList.get(j));
//								}
                                cnt += freqList.get(j);
                            }
                        }
                    }

                }
            });
            // threads[i].start();
        }
        final long testStartTime = System.nanoTime();
        for (int i = 0; i < threadnum; i++) {
            threads[i].start();
        }
        for (int i = 0; i< threadnum; i++) {
            threads[i].join();
        }
        final long testFinishTime = System.nanoTime();
        int totalTestNum = testnum * threadnum ;
        long actualUseNs = testFinishTime - testStartTime - totalTestNum * nsec - totalTestNum * msec * 1000000L;
        long timeUse = actualUseNs/(1000000L);
        System.out.println("Multi Thread Test Using: " + timeUse + " ms");
        System.out.println("threadnum:" + threadnum + " pos/thread: " + totalTestNum/timeUse + " op/ms");
    }


    public static void main(String[] args) throws InterruptedException {
        threadnum = 3;
        testnum = 50;
        multiThreadTest();
        printHistory();
        verify();
    }
}
