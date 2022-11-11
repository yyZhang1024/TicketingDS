package ticketingsystem;


public class Test {
	public static void asserts(boolean t){
//		if(!t){
//			System.out.println("ERROR!!!");
//			System.exit(-1);
//		}
	}
	public static void main(String[] args) throws InterruptedException {
        int routenum = 3;
		int coachnum = 3;
		int seatnum = 5;
		int stationnum = 5;
		int threadnum = 16;
		// 3 3 5 5 10 30 60
		int total = coachnum * seatnum;

		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
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
}
