/*
 *  File: [SimpleFraction.java]
 *  By:   [Artsem Holdvekht]
 *  Date: [22 Jul 16]
*/

package PJ3;

import java.util.*;
import java.io.*;

// You may add new functions or data fields in this class 
// You may modify any functions or data members here
// You must use Customer, Cashier and CheckoutArea classes
// to implement SuperMart simulator

class SuperMart {
    // input parameters
    private int numCashiers, customerQLimit;
    private int chancesOfArrival, maxServiceTime;
    private int simulationTime, dataSource;

    // statistical data
    private int numGoaway, numServed, totalWaitingTime;

    // internal data
    private int counter;                // customer ID counter
    private CheckoutArea checkoutarea;  // checkout area object
    private Random dataRandom;          // get customer data using random function
    private Scanner userInput;          // get user input
    private Scanner dataFile;           // get customer data from file

    // most recent customer arrival info, see getCustomerData()
    private boolean anyNewArrival;
    private int serviceTime;

    // initialize data fields
    private SuperMart()
    {
        counter = 1;
        numGoaway = 0;
        userInput = new Scanner(System.in);
    }

    private void setupParameters() {
        System.out.println("\t*** Get Simulation Parameters ***\n");

        System.out.print("Enter simulation time (positive integer): ");
        simulationTime = Integer.parseInt(userInput.next());

        System.out.print("Enter the number of cashiers: ");
        numCashiers = Integer.parseInt(userInput.next());

        System.out.print("Enter chances (0% < & <= 100%) of new customer: ");
        chancesOfArrival = Integer.parseInt(userInput.next());

        System.out.print("Enter maximum service time of customers: ");
        maxServiceTime = Integer.parseInt(userInput.next());

        System.out.print("Enter customer queue limit: ");
        customerQLimit = Integer.parseInt(userInput.next());

        System.out.print("Enter 0/1 to get data from random/file: ");
        dataSource = Integer.parseInt(userInput.next());

        //I did not include the check for integers, so anything but 1 will be from random file
        if(dataSource == 1)
        {
            System.out.print("Enter filename: ");
            String fileName = userInput.next();
            readFile(fileName);
            getCustomerData();
        }
        else
        {
            getCustomerData();
        }
    }//end setupParameters


    private void readFile(String filename)
    {
        try
        {
            dataFile = new Scanner(new File(filename));
        }
        catch(FileNotFoundException e)
        {
            System.out.println("File Not Found");
            readFile(filename);
        }
    }//end readFile

    // get next customer data : from file or random number generator
    private void getCustomerData() {
        // set anyNewArrival and serviceTime
        if(dataSource == 1)
        {

            try
            {
                int nextInt = dataFile.nextInt();
                anyNewArrival = (((nextInt % 100) + 1) <= chancesOfArrival);
                serviceTime = ((nextInt % maxServiceTime) + 1);
            }
            catch(NoSuchElementException e)
            {
                System.out.println("There was an error reading input from the file or we ran out of data in the file!");
            }
        }
        else
        {
            dataRandom = new Random();
            anyNewArrival = ((dataRandom.nextInt(100)+1) <= chancesOfArrival);

            serviceTime = dataRandom.nextInt(maxServiceTime) + 1;
        }
    }//end getCustomerData

    private void freeBusyCashiers(CheckoutArea checkoutarea, int currentTime)
    {
        while(!checkoutarea.emptyBusyCashierQ())
        {
            Cashier busyCashier = checkoutarea.peekBusyCashierQ();

            Customer currentCustomer = busyCashier.getCurrentCustomer();
            int customerServiceTime = (currentCustomer.getArrivalTime() - currentCustomer.getServiceTime());

            if(customerServiceTime <= currentTime)
            {
                busyCashier = checkoutarea.removeBusyCashierQ();
                System.out.println("\tCashier #" + busyCashier.getCashierID() + " is free");

                Customer customer = busyCashier.busyToFree();
                System.out.println("\tCustomer #" + customer.getCustomerID() + " is done");


                checkoutarea.insertFreeCashierQ(busyCashier);
            }
            else
            {
                break;
            }

        }
    }

    private void freeCashierToBusy(CheckoutArea checkoutarea, int currentTime)
    {
        System.out.println("\tCustomer #" +counter+ " gets a cashier");

        //remove cashier form free que
        Cashier cashier = checkoutarea.removeFreeCashierQ();

        //remove customer from waiting que
        Customer customer = checkoutarea.removeCustomerQ();

        //Transition from free interval to busy interval
        cashier.freeToBusy(customer, currentTime);

        //update BusyTime
        cashier.setEndBusyTime(currentTime + serviceTime);

        //add cashier to busy que
        checkoutarea.insertBusyCashierQ(cashier);

        //output status update
        System.out.println("\tCashier #" + cashier.getCashierID() + " starts serving customer #" + counter+ " for " + serviceTime + " units");
    }// end freeCashierToBusy

    private void doSimulation()
    {
        System.out.println("\n\t*** Start Simulation ***\n");

        // Initialize CheckoutArea
        checkoutarea = new CheckoutArea(numCashiers, customerQLimit);

        System.out.println("\nCashiers #1 to #" + numCashiers + " are ready...");

        // Time driver simulation loop
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            System.out.println("---------------------------------------------------");
            System.out.println("Time : " + currentTime);

            //getCustomerData for the rotation
            getCustomerData();

            // Step 1: any new customer enters the checkout area?
            if (anyNewArrival)
            {
                // Step 1.1: setup customer data
                Customer customer = new Customer(counter, serviceTime, currentTime);
                System.out.println("\tcustomer #" + counter + " arrives with checkout time " +serviceTime+ " units");

                // Step 1.2: check customer waiting queue too long?
                if(!checkoutarea.isCustomerQTooLong())
                {
                    checkoutarea.insertCustomerQ(customer);
                    System.out.println("\tcustomer#" +counter + " waits in the customer queue");
                }
                else
                {
                    System.out.println("\tcustomer#" +counter + " leaves since the customer queue is full");
                    numGoaway++;
                }
            }
            else
            {
                System.out.println("\tNo new customer!");

            }

            // Step 2: free busy cashiers, add to free cashierQ
            freeBusyCashiers(checkoutarea, currentTime);

            // Step 3: get free cashiers to serve waiting customers
            if(!checkoutarea.emptyCustomerQ() && !checkoutarea.emptyFreeCashierQ())
            {
                freeCashierToBusy(checkoutarea, currentTime);
                counter++;

            }
        } // end of for simulation loop
    }//end of doSimulation

    private void printStatistics()
    {
        // print out simulation results
        // see the given example in project statement
        // you need to display all free and busy cashiers

        System.out.println("---------------------------------------------------");
        System.out.println("End of Simulation report");
        System.out.println("\n\t# total arrival customers: " + numServed);
        System.out.println("\t# customers gone-way: " + numGoaway);
        System.out.println("\t# customers served: " + numServed);

        System.out.println("\n\t*** Current Cashiers Info. ***");
        System.out.println("\t# Waiting Customers: " + checkoutarea.sizeCustomerQ());
        System.out.println("\t# busy cashiers: " + checkoutarea.sizeBusyCashierQ());
        System.out.println("\t# free cashiers: " + checkoutarea.sizeFreeCashierQ());


        //output busy cashier info
        System.out.println("\n\tBusy Cashiers Info. :");

        while(!checkoutarea.emptyBusyCashierQ())
        {
            Cashier busy = checkoutarea.removeBusyCashierQ();
            busy.printStatistics();
        }

        //output free cashier info
        System.out.println("\n\tFree Cashier Info. :");

        while(!checkoutarea.emptyFreeCashierQ())
        {
            Cashier free = checkoutarea.removeFreeCashierQ();
            free.printStatistics();
        }

    } //end printStatistics

    // *** main method to run simulation ****

    public static void main(String[] args)
    {

        SuperMart runSuperMart = new SuperMart();
        runSuperMart.setupParameters();
        runSuperMart.doSimulation();
        runSuperMart.printStatistics();
    }//end main

}//end SuperMart Class
