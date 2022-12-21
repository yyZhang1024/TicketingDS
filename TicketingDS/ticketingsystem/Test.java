package ticketingsystem;


import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class Test {
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

	final static List<Long> buyNum = new ArrayList<Long>();
	final static List<Long> buyTime = new ArrayList<Long>();


	final static List<Long> inquiryNum = new ArrayList<Long>();
	final static List<Long> inquiryTime = new ArrayList<Long>();

	final static List<Long> refundNum = new ArrayList<Long>();
	final static List<Long> refundTime = new ArrayList<Long>();

	static List<String> logs = new LinkedList<>();
	final static ArrayList<List<Ticket>> soldTicket = new ArrayList<List<Ticket>>();
	public static String getPassengerName() {
		long uid = rand.nextInt(testnum);
		return "passenger" + uid;
	}
	public static String ticketToString(Ticket ticket) {
		return ticket.passenger + " tid: " + ticket.tid + " route: " + ticket.route + " coach: " + ticket.coach + " seat: " + ticket.seat + " departure: " + ticket.departure + " arrival: " + ticket.arrival;
	}
	public static void sampleSeqTest(){
		routenum = 1;
		coachnum = 1;
		seatnum = 5;
		threadnum = 1;
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
		Ticket t1 = tds.buyTicket("passenger383", 1, 2, 3);
		System.out.println(ticketToString(t1));
		//5
		System.out.println(tds.inquiry(1, 1, 2));

		Ticket t2 = tds.buyTicket("passenger383", 1, 1, 2);
		System.out.println(ticketToString(t2));
		//5
		System.out.println(tds.inquiry(1, 3, 4));


		Ticket t3 = tds.buyTicket("passenger383", 1, 3, 4);
		System.out.println(ticketToString(t3));
		System.out.println(tds.inquiry(1, 1, 5));


		System.out.println(ticketToString(t1));
		tds.refundTicket(t1);
		System.out.println(tds.inquiry(1, 2, 3));
	}

	public static void initialization() {
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
		for(int i = 0; i < threadnum; i++){
			List<Ticket> threadTickets = new ArrayList<Ticket>();
			soldTicket.add(threadTickets);
			currentTicket.add(null);
			currentRes.add("");
			currentNum.add(-1);
			inquiryTime.add(0L);
			inquiryNum.add(0L);
			buyNum.add(0L);
			buyTime.add(0L);
			refundNum.add(0L);
			refundTime.add(0L);
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
				return flag;
			case 1://buy
				String passenger = getPassengerName();
				route = rand.nextInt(routenum) + 1;
				departure = rand.nextInt(stationnum - 1) + 1;
				arrival = departure + rand.nextInt(stationnum - departure) + 1;
				ticket = tds.buyTicket(passenger, route, departure, arrival);
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
	public static void print(long preTime, long postTime, String actionName){
		Ticket ticket = currentTicket.get(ThreadId.get());
		String s = preTime + " " + postTime + " " +  ThreadId.get() + " " + actionName + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat + " " + currentRes.get(ThreadId.get());
		logs.add(s);
		System.out.println(s);
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
								if(flag){
									int threadid = ThreadId.get();
									if (methodList.get(j) == "inquiry") {
										inquiryNum.set(threadid, inquiryNum.get(threadid) + 1);
										inquiryTime.set(threadid, inquiryTime.get(threadid) + (postTime - preTime));
									} else if (methodList.get(j) == "buyTicket") {
										buyNum.set(threadid, buyNum.get(threadid) + 1);
										buyTime.set(threadid, buyTime.get(threadid) + (postTime - preTime));
									} else {
										refundNum.set(threadid, refundNum.get(threadid) + 1);
										refundTime.set(threadid, refundTime.get(threadid) + (postTime - preTime));
									}
								}
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
		long totalbuyTime = 0;
		long totalrefundTime = 0;
		long totalinquiryTime = 0;
		long totalbuyNum = 0;
		long totalrefundNum = 0;
		long totalinquiryNum = 0;
		for (int i = 0; i< threadnum; i++) {
			totalbuyTime += buyTime.get(i);
			totalrefundTime += refundTime.get(i);
			totalinquiryTime += inquiryTime.get(i);

			totalbuyNum += buyNum.get(i);
			totalrefundNum += refundNum.get(i);
			totalinquiryNum += inquiryNum.get(i);
		}

		long avgBuyTime = totalbuyTime / totalbuyNum;
		long avgRefundTime = totalrefundTime / totalrefundNum;
		long avgInquiryTime = totalinquiryTime / totalinquiryNum;

		System.out.println("Multi Thread Test Using: " + timeUse + " ms AverageBuyTime: " + avgBuyTime + "ns AverageRefundTime: " + avgRefundTime + "ns AverageInquiryTime: " + avgInquiryTime + "ns ThreadNum:" + threadnum + " Throughput: " + totalTestNum/timeUse + " ops/ms");
	}
	static void generateAllState() {
		int k = 0;
		for (int departure = 0; departure < 29; departure++) {
			for (int arrival = departure + 1; arrival < 30; arrival++) {
				int occupyBits = 0;
				int base = 2 << departure;
				for (int i = departure; i < arrival; i++) {
					occupyBits |= base;
					base <<= 1;
				}
				// System.out.println(occupyBits + " departure: " + departure + " arrival:" + arrival + " total:" + k + " test: " + ((departure*(30 + 30 - departure + 1) / 2) + arrival - 2 * departure - 1));
				// System.out.println(occupyBits+",");
				System.out.println(occupyBits + " departure: " + departure + " arrival:" + arrival + " total:" + k + " test: " + ((departure*(57 - departure) / 2) + arrival - 1));

				k++;
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {


		if (true) {
			needOutput = false;
			// generateAllState();
			// sampleSeqTest();
			threadnum = 3;
			testnum = 50;
			multiThreadTest();
//			verify();
		} else {
			needOutput = false;
			// generateAllState();
			// sampleSeqTest();
			threadnum = Integer.parseInt(args[0]);
			testnum = Integer.parseInt(args[1]);
			multiThreadTest();
		}
	}
}
