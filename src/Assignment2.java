import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/***************************************************************************************/
// NAME : KARUN MALIK
// ID : 7842613

/* the main class of assignment 2, launching the simulation */
public class Assignment2 {
    // Configuration
    final static int DESTINATIONS = 4;
    final static int AEROPLANES = 6;
    final static int PLANE_SIZE = 3;
    final static int PASSENGERS = 20;
    final static String[] destName = {"Toronto", "New York", "New Delhi", "Beijing"};
    public static void main(String args[]){
        int i;
        Aeroplane[] sships = new Aeroplane[AEROPLANES];
        Passenger[] passengers = new Passenger[PASSENGERS];

        // create the airport
        Airport sp = new Airport();

        /* create aeroplanes and passengers*/
        for (i=0; i<AEROPLANES; i++)
            sships[i] = new Aeroplane(sp, i);
        for (i=0; i<PASSENGERS; i++)
            passengers[i] = new Passenger(sp, i);

        /* now launch them */
        for (i=0; i<AEROPLANES; i++)
            sships[i].start();
        for (i=0; i<PASSENGERS; i++)
            passengers[i].start();

        // let them enjoy for 20 seconds
        try { Thread.sleep(20000);} catch (InterruptedException e) { }

        /* now stop them */
        // note how we are using deferred cancellation
        for (i=0; i<AEROPLANES; i++)
            try {sships[i].interrupt();} catch (Exception e) { }
        for (i=0; i<PASSENGERS; i++)
            try {passengers[i].interrupt();} catch (Exception e) { }

        // Wait until everybody else is finished
        // your code here
        System.out.println("Joining threads");
        for (i=0; i<PASSENGERS; i++) {
                 try {
                     passengers[i].join();
                 } catch (InterruptedException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
             }
        for (i=0; i<AEROPLANES; i++) {
            try {
                sships[i].join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        System.out.println("Threads successfully joined!!");
        // This should be the last thing done by this program:
        System.out.println("Simulation finished.");
    }
}



/* The class implementing a passenger. */
// This class is completely provided to you, you don't have to change
// anything, just have a look and understand what the passenger wants from
// the airport and from the aeroplanes
class Passenger extends Thread {
    private boolean enjoy;
    private int id;
    private Airport sp;
 

    // constructor
    public Passenger(Airport sp, int id) {
        this.sp = sp;
        this.id = id;
        enjoy = true;
       
    }

    // this is the passenger's thread
    public void run() {
        int         stime;
        int         dest;
        Aeroplane   sh;

        while (enjoy) {
            try {
                // Wait and arrive to the port
                stime = (int) (700*Math.random());
                sleep(stime);

                // Choose the destination
                dest = (int) (((double) Assignment2.DESTINATIONS)*Math.random());
                System.out.println("Passenger " + id + " wants to go to " + Assignment2.destName[dest]);

                // come to the airport and board a aeroplane to my destination
                // (might wait if there is no such aeroplane ready)
                sh = sp.wait4Ship(dest);

                // Should be executed after the aeroplane is on the pad and taking passengers
                System.out.println("Passenger " + id + " has boarded aeroplane " + sh.id + ", destination: "+Assignment2.destName[dest]);

                // wait for launch
                sh.wait4launch();

                // Enjoy the ride
                // Should be executed after the aeroplane has launched.
                System.out.println("Passenger "+id+" enjoying the ride to "+Assignment2.destName[dest]+ ": Woohooooo!");

                // wait for landing
                sh.wait4landing();

                // Should be executed after the aeroplane has landed
                System.out.println("Passenger " + id + " leaving the aeroplane " + sh.id);

                // Leave the aeroplane
                sh.leave();
            } catch (InterruptedException e) {
      
                enjoy = false; // have been interrupted, probably by the main program, terminate
            }
       }
       System.out.println("Passenger "+id+" has finished its rides.");
    }
}

/* The class simulating an aeroplane */
// Now, here you will have to implement several methods
class Aeroplane extends Thread {
    public int  id ;
    private Airport    sp ;
    private boolean enjoy ;
    private int passengers_in_plane ; // passengers currently in plane
    private Semaphore sem_plane_takeoff ; // semaphore to signal the aeroplane that passengers have boarded plane and is ready to take-off
    private Semaphore sem_wait_for_passenger ; // semaphore used to wait until all passengers board the plane
    private Semaphore sem_sleep_passenger; // semaphore used to wakeup/sleep passengers
  
    // constructor
    public Aeroplane(Airport sp, int id) {
        this.sp = sp;
        this.id = id;
        enjoy = true;
        passengers_in_plane = 0;
        sem_sleep_passenger = new Semaphore(0);// initialization
        sem_plane_takeoff = new Semaphore(0);
        sem_wait_for_passenger = new Semaphore(0);
    }

    // the aeroplane thread executes this
    public void run() {
        int     stime;
        int     dest;

        while (enjoy) {
            try {
                // Wait until there an empty landing pad, then land
                dest = sp.wait4landing(this);

                System.out.println("Aeroplane " + id + " landing on pad " + dest);

             // Tell the passengers that we have landed
            
               if(passengers_in_plane!=0){
                    sem_sleep_passenger.signalSem();// wake up all slept passengers
                    sem_sleep_passenger.signalSem();
                    sem_sleep_passenger.signalSem();
                    sem_wait_for_passenger.waitSem();// wait for passenger to leave the plane
               }
                System.out.println("Aeroplane " + id + " boarding to "+Assignment2.destName[dest]+" now!");
                // the passengers can start to board now
               sp.boarding(dest);
               sem_plane_takeoff.waitSem(); // wait for passengers to board the plane
               
                // 4, 3, 2, 1, Launch!
               
                System.out.println("Aeroplane " + id + " launches towards "+Assignment2.destName[dest]+"!");
                sp.launch(dest);
                // tell the passengers we have launched, so they can enjoy now ;-)
                sem_sleep_passenger.signalSem(); // wake up slept passengers
                sem_sleep_passenger.signalSem();
                sem_sleep_passenger.signalSem();
                // Fly in the air
                stime = 500+(int) (1500*Math.random());
                sleep(stime);
            } catch (InterruptedException e) {
                enjoy = false; // have been interrupted, probably by the main program, terminate
            }
        }
        System.out.println("Aeroplane "+id+" has finished its flights.");
    }
    
    // service functions to passengers
    // called by the passengers leaving the aeroplane
    public void leave()  throws InterruptedException  {
    	 passengers_in_plane--; // tracking number of passengers leaving plane
        if(passengers_in_plane==0){
            sem_wait_for_passenger.signalSem(); // signal the plane that all passengers have left the plane
        }      
    }

    // called by the passengers sitting in the aeroplane, to wait
    // until the launch
    public void wait4launch()  throws InterruptedException {
         passengers_in_plane++;  // keep track of total passengers entering the plane
        if(passengers_in_plane==Assignment2.PLANE_SIZE){
        sem_plane_takeoff.signalSem(); // signal the plane that flight is full
        }
       
         sem_sleep_passenger.waitSem(); // passengers are waiting for launch
    }

    // called by the bored passengers sitting in the aeroplane, to wait
    // until landing
    public void wait4landing()  throws InterruptedException {
       
    	sem_sleep_passenger.waitSem(); // passengers are waiting for landing
}
}

/* The class implementing the Airport. */
/* This might be convenient place to put lots of the synchronization code into */
class Airport {
    Aeroplane[]    pads; // what is sitting on a given pad
    private Semaphore[] sem_passenger_dest ; 
    private Queue<Integer> free_runway_queue ;  // keeps track of rotation of pads so no destination is starved. I found this better than using random method


    private Semaphore sem_runways; // semaphore to keep track of available free runways
    // constructor
    public Airport() {
        int i;
        sem_runways = new Semaphore(Assignment2.DESTINATIONS);
        sem_passenger_dest = new Semaphore[Assignment2.DESTINATIONS];
        free_runway_queue = new LinkedList<Integer>();
        pads = new Aeroplane[Assignment2.DESTINATIONS];

        // pads[] is an array containing the aeroplanes sitting on corresponding pads
        // Value null means the pad is empty
        for(i=0; i<Assignment2.DESTINATIONS; i++) {
            pads[i] = null;
            sem_passenger_dest[i] = new Semaphore(0);
            free_runway_queue.add(i); 
        }
    }

    // called by a passenger wanting to go to the given destination
    // returns the aeroplane he/she boarded
    // Careful here, as the pad might be empty at this moment
    public Aeroplane wait4Ship(int dest) throws InterruptedException {
     
        sem_passenger_dest[dest].waitSem(); // wait at the terminal until boarding begins
        return (pads[dest]);
       
    }
    // called by an aeroplane to tell the airport that it is accepting passengers now to destination dest
    public void boarding(int dest) {
        if(pads[dest]!=null) {
            sem_passenger_dest[dest].signalSem(); // Allow 3 passengers to pass through terminal. (Issue only 3 boarding passes)
            sem_passenger_dest[dest].signalSem();
            sem_passenger_dest[dest].signalSem();
        }
    }

    // Called by an aeroplane returning from a trip
    // Returns the number of the empty pad where to land (might wait
    // until there is an empty pad).
    // Try to rotate the pads so that no destination is starved
    public int wait4landing(Aeroplane sh) throws InterruptedException  {
        sem_runways.waitSem(); // wait until there is an available free runway
        int pad=free_runway_queue.remove(); // take out the free runway
        pads[pad]=sh; // assign aeroplane
        return pad;
    }

    // called by an aeroplane when it launches, to inform the
    // airport that the pad has been emptied
    public void launch(int dest) {
    	free_runway_queue.add(dest); // when plane is launched add that runway to the queue
        pads[dest]=null; // null the pad
        sem_runways.signalSem();      
    }
}