package ticketingsystem;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {
	public static void asserts(boolean t){
//		if(!t){
//			System.out.println("ERROR!!!");
//			System.exit(-1);
//		}
	}
	final static Random rand = new Random();
	static int testnum = 50;
	static int routenum = 3;
	static int coachnum = 3;
	static int seatnum = 5;
	static int stationnum = 5;
	static int threadnum = 16;
	static int msec = 0;
	static int nsec = 0;
	static int refRatio = 10;
	static int buyRatio = 20;
	static int inqRatio = 30;
	static int totalPc;
	static TicketingDS tds;
	volatile static boolean initLock = false;
	final static List<String> methodList = new ArrayList<String>();
	final static List<Integer> freqList = new ArrayList<Integer>();
	final static List<Ticket> currentTicket = new ArrayList<Ticket>();
	final static List<String> currentRes = new ArrayList<String>();
	final static ArrayList<List<Ticket>> soldTicket = new ArrayList<List<Ticket>>();
	public static String getPassengerName() {
		long uid = rand.nextInt(testnum);
		return "passenger" + uid;
	}
	public static void sampleSeqTest(){
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
		// 7640354 8705514 0 buyTicket 1 passenger45 3 1 3 4 1 true
		// 8539293 9282449 3 inquiry 0 passenger87 1 0 4 5 15 true
		// 9550221 9613249 3 inquiry 0 passenger61 1 0 3 5 15 true
		// 9866533 9996782 3 inquiry 0 passenger42 3 0 4 5 15 true
		// 10846476 10934945 3 inquiry 0 passenger68 1 0 4 5 15 true

//		tds.buyTicket("passenger45", 3, 1, 3);
		/*
		*
(preTime + " " + postTime + " " +  ThreadId.get() + " " + actionName + " " + ticket.tid + " " + ticket.passenger + " " +
ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat + " " + currentRes.get(ThreadId.get()));

10254734 11262772 0 inquiry 0 passenger131 2  1 2 		15
11663277 11873954 1 buyTicket 1 passenger383 1  2 5 	1
12222903 12400913 3 inquiry 0 passenger346 1  2 5 		14
*
12641940 12716595 0 buyTicket 2 passenger453 1  4 5 	2
12869852 12941320 0 inquiry 0 passenger844 1  2 4 		14
*
13047321 13154080 1 refundTicket 1 passenger383 1  2 5 	1
13308481 13410324 2 inquiry 0 passenger116 3  2 3 		15
13616778 14142327 2 inquiry 0 passenger214 2  4 5 		15
*
14338380 14385823 1 buyTicket 3 passenger388 1  2 3 	3
14540932 14618285 2 inquiry 0 passenger338 3  2 4 		15
14762389 14839476 3 inquiry 0 passenger812 3  4 5 		15
14972814 15051194 3 inquiry 0 passenger509 1  3 4 		15
		* */
		asserts(tds.inquiry(2, 1,2) == 15);
		Ticket t1 = tds.buyTicket("passenger383", 1, 2, 5);
		asserts(tds.inquiry(1, 2,5) == 14); // should 14

		Ticket t2 = tds.buyTicket("passenger453", 1, 4, 5);
		asserts(tds.inquiry(1, 2,4) == 14); // should 14

		tds.refundTicket(t1);
		asserts(tds.inquiry(3, 2,3) == 15);
		asserts(tds.inquiry(2, 4,5) == 15);

		asserts(tds.inquiry(1, 3,4) == 15);
		Ticket t3 = tds.buyTicket("passenger388", 1, 2, 3);
		asserts(tds.inquiry(3, 2,4) == 15);
		asserts(tds.inquiry(3, 4,5) == 15);
		// 左闭右开区间，这里又不是左闭又开了？？？？？
		asserts(tds.inquiry(1, 3,4) == 14); // should eq 14
		//ToDo
	}

	public static void initialization() {
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
		for(int i = 0; i < threadnum; i++){
			List<Ticket> threadTickets = new ArrayList<Ticket>();
			soldTicket.add(threadTickets);
			currentTicket.add(null);
			currentRes.add("");
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
		System.out.println(preTime + " " + postTime + " " +  ThreadId.get() + " " + actionName + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat + " " + currentRes.get(ThreadId.get()));
	}
	// output through
	public static void multiThreadTest() throws InterruptedException {
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
									print(preTime, postTime, methodList.get(j));
								}
								cnt += freqList.get(j);
							}
						}
					}

				}
			});
			threads[i].start();
		}
		for (int i = 0; i< threadnum; i++) {
			threads[i].join();
		}
	}
	public static void main(String[] args) throws InterruptedException {
        routenum = 3;
		coachnum = 3;
		seatnum = 5;
		stationnum = 5;
		threadnum = 2;

		// sampleSeqTest();
		multiThreadTest();
	}
}
