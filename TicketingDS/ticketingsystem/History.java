package ticketingsystem;

public class History implements Comparable{

    public long preTime;
    public long postTime;
    public String action;
    public int route;
    public int coach;
    public int departure;
    public int arrival;
    public int seat;
    public long tid;
    public int threadID;
    String passenger;
    boolean flag;
    String result;
    Integer currentNum;
    public  History(long preTime, long postTime, String action, int route, int coach, int departure, int arrival, int seat, long tid, int threadID, String passenger, boolean flag, String result, Integer currentNum) {
        this.preTime = preTime;
        this.postTime = postTime;
        this.action = action;
        this.route = route;
        this.coach = coach;
        this.departure = departure;
        this.arrival = arrival;
        this.seat = seat;
        this.tid = tid;
        this.threadID = threadID;
        this.passenger = passenger;
        this.flag = flag;
        this.result = result;
        this.currentNum = currentNum;
    }

    @Override
    public int compareTo(Object o) {
        History r = (History) o;
        if(this.preTime == r.preTime) {
            return this.postTime < r.postTime? 1 : 0;
        } else {
            return this.preTime < r.preTime? 1 : 0;
        }
    }
}
