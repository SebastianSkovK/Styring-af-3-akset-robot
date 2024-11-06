import edgedetect.Picture;
import edgedetect.EdgeDetector;

import java.util.Arrays;

public class Main {
    //##load image from file
    String url = "https://static.europelanguagejobs.com/contents/companies/280856/100-5f292a0c93e6c.jpg"; // Apple logo 100x100 pixels'
    //String url = "file:///C:/Users/rdkla/OneDrive%20-%20Syddansk%20Universitet/Semesterprojekt-01semester/apple.jpg"; // Apple logo 100x100 pixels, med sorte kanter så det ikke kan komprimeres
    //String url = "file:///C:/Users/rdkla/Pictures/Nikolaj_Jarl_350x262.png"; //350x262 Nicolaj Billede

    //String url = "file:///C:/Users/rdkla/Pictures/IMG_20221209_103520_Bokeh.jpg"; //Nicolaj Billede ligger ned H: 165 , V: 220
    //String url = "file:///C:/Users/rdkla/Pictures/IMG_20221209_103520_stand_up.png"; //Nicolaj Billede står op H: 220 , V: 165

    //String url = "https://www.flashscore.ca/res/image/data/bkmgMCCr-EPd4m8G0.png";  //team liquid 100x100
    //String url = "https://www.wikihow.com/images/c/cd/Android7chrome.png"; //chrome 30x30
    //String url = "https://damcache.harald-nyborg.dk/v-637728230064908874/0c/c6/370c-edda-4828-9d7e-cacc504bb218/duck.jpg"; //200x200 Duck
    //String url = "https://www.metoperashop.org/prodimages/6763-DEFAULT-l.jpg"; //400x400 Viking Duck
    Picture image = new Picture(url);


    RobotClient robot = new RobotClient("127.0.0.1", 12345); //Sim, creates instance of robotClient class, to connect to PLC later.
    //RobotClient robot = new RobotClient("192.168.1.5", 12345); //PLC, creates instance of robotClient class, to connect to PLC later.


    EdgeDetector readImage = new EdgeDetector(url); //makes an EdgeDetector class with the chosen image.
    int imageHeight = 0;
    int imageWidth = 0;

    int[][] imageMagnitudeArray; //array for magnitude of image
    public int[][] imageArray;  //int array

    public String[] imageStringArray; //full picture
    public String[] newImageStringArray; //compressed without zero lines at top and bottom

    public String startData = "";
    int rows; //number of times to sende data for each image height.
    int intSizeString;
    int counterEmptyTop = 0; //count number of empty top lines

    boolean imageIsFliped = false; //it's true if the image have been fliped.
    String finalStringArray[]; //final string array to send to the robot

    /**
     * Saves the image height and width to the class varabels, so they can be used in other methods, to save lots of compute time.
     * Runs readImage.getMagnitudeArray() and saves the output array with grayscale values to this.imageMagnitudeArray.
     *
     * 55 |55 |55
     * 255|255|255
     * 255|255|255
     */
    public void runGetMagnitudeArray(){
        image.show();
        System.out.println("image height: " + image.height() + " , " + "width: " + image.width());
        this.imageHeight = image.height()-2;  //-2 da vi mister 2 pixels fra EdgeDetector klassen
        this.imageWidth = image.width()-2;  //-2 da vi mister 2 pixels fra EdgeDetector klassen
        System.out.println("image height: " + this.imageHeight + " , " + "width: " + this.imageWidth);
        this.imageMagnitudeArray = readImage.getMagnitudeArray(); //saves to global attribut
    }

    /**
     * Converts the imageArray from grayscale to Black & White also known as binary.
     *
     * [55 ,55 ,55 ] -> [1,1,1]
     * [170,185,180] -> [1,1,1]
     * [255,255,255] -> [0,0,0]
     */
    public void convertToBinaray(){
        int imageSaveSetting = 0; //0 = standart //1 = //2 =

        int rangeI = this.imageHeight;
        int rangeJ = this.imageWidth;

        if(this.imageWidth == this.imageHeight || this.imageHeight < this.imageWidth){
            imageSaveSetting = 0;
            this.imageIsFliped = false;
            System.out.println("imageSaveSetting: " + imageSaveSetting);

            this.imageArray = new int[this.imageHeight][this.imageWidth];

        }
        else if (this.imageHeight > this.imageWidth) {  //if the height is bigger than the width, then we flip the image 90 degrees
            imageSaveSetting = 1;
            this.imageIsFliped = true;
            System.out.println("imageSaveSetting: " + imageSaveSetting);
            int height = this.imageHeight; //makes a copy
            int width = this.imageWidth; //makes a copy

            this.imageArray = new int[this.imageWidth][this.imageHeight];

            this.imageHeight = width; //changes the value because we rotated the image 90 degrees
            this.imageWidth = height; //changes the value because we rotated the image 90 degrees

        }

        System.out.println("is image fliped: " + this.imageIsFliped);

        ////runs through imageMagnitudeArray that contains grayscale values for each pixel, and saves as 1 or 0 to imageArray
        for(int i = 0; i < rangeI; i++){
            for(int j = 0; j < rangeJ; j++){
                //grayscale from 0 to 256
                if (this.imageMagnitudeArray[j][i] > 240){  //saves as white if grayscale values over 240
                    if(this.imageIsFliped){
                        this.imageArray[j][i] = 0; //white  //saves at position [j][i] to turn the array so the image is rotated

                    } else {  //image not fliped
                        this.imageArray[i][j] = 0; //white
                    }
                }
                else{  //saves as black
                    if(this.imageIsFliped){
                        this.imageArray[j][i] = 1; //saves at position [j][i] to turn the array so the image is rotated

                    } else {  //image not fliped
                        this.imageArray[i][j] = 1;
                    }
                }
            }
        }
    }

    /**
     * Removes the white tale from a given string.
     * example "11000011110000" turns to "1100001111"
     */
    public String removeWhiteEndFromString(String inString){
        String outputString = "";
        boolean pixelMeet = false;
        for(int i = inString.length()-1; i > 0; i--){
            if(inString.charAt(i) == '1'){
                pixelMeet = true;
                outputString = String.valueOf(inString.toCharArray(), 0, i+1);
                break;
            }
        }
        return outputString;
    }

    /**
     * Converts the imageArray[][] to a string[] and saves it to finalStringArray[]
     * in the proces it removes '0' line from Top, Bottom, Left and Right to remove unnecessary data.
     */
    public void convertToString(){
        ////removes 0 lines from top
        this.imageStringArray = new String[this.imageHeight]; //creates a new array with the same height as the image
        boolean lineWithOneFromTop = false; //if a line with a 1 is found, then this is set to true

        for(int i = 0; i < this.imageHeight; i++){ //runs through the imageArray
            String imageString = ""; //creates a new empty string for each line
            for (int j = 0; j < this.imageWidth; j++) { //runs through the imageArray
                imageString += imageArray[i][j]; //adds the value of the pixel to the string
                if (imageArray[i][j] == 1) { //if a pixel with a 1 is found
                    lineWithOneFromTop = true; //sets the boolean to true

                }
            }
            System.out.println(imageString); //prints the string to the java terminal
            if(lineWithOneFromTop){ //if a line with a 1 is found
                this.imageStringArray[i] = imageString; //adds the string to the imageStringArray

            } else { //if no line with a 1 is found
                this.counterEmptyTop += 1; //adds 1 to the counter of empty lines from top
            }
        }
        System.out.println("counterEmptyTop: " + this.counterEmptyTop);


        //////removes 0 lines from bottom of image
        boolean lineWithOneFromBottom = false;  //if a line with a '1' is found, then this is set to true
        int bottomLineFoundWithOne = this.imageHeight-1; //holds the value of the last line with a '1' in it
        String readString; //holds the string that is read from the imageStringArray

        for(int i = this.imageStringArray.length-1; i > this.counterEmptyTop; i--){ //runs through the imageStringArray from bottom to top
            readString = this.imageStringArray[i]; //reads the string from the imageStringArray

            for(int j = 0; j < this.imageWidth; j++){ //runs through the string from left to right
                if(readString.charAt(j) == '1'){ //if a '1' is found
                    lineWithOneFromBottom = true; //sets the boolean to true
                    break; //breaks the for loop
                }
            }
            if(!lineWithOneFromBottom){ //if no '1' is found in the string
                bottomLineFoundWithOne -= 1; //decreases the value of the last line with a '1' in it

            } else {
                break; //breaks the for loop
            }
        }

        System.out.println("bottomLineFoundWithOne: " + bottomLineFoundWithOne);


        //////saves to new string array without 0 lines
        this.newImageStringArray = new String[bottomLineFoundWithOne-this.counterEmptyTop+1];  //creates new string array
        //this.newImageStringArray = new String[bottomLineFoundWithOne];  //creates new string array


        int saveCounter = 0; //counter for the new string array
        System.out.println("strings without zero line on top and buttom");
        //for(int i = this.counterEmptyTop; i < bottomLineFoundWithOne+1; i++){

        int valueToAdd = 0; //holds the value to add to the counter
        if(this.counterEmptyTop == 0){ //if there is no empty lines from top
            valueToAdd = 0;
        } else { //if there is empty lines from top
            valueToAdd = 1;
        }

        //for(int i = this.counterEmptyTop; i < bottomLineFoundWithOne+1; i++){
        if(!this.imageIsFliped) { //if the image is not flipped
            for (int i = this.counterEmptyTop; i < bottomLineFoundWithOne + valueToAdd; i++) {
                this.newImageStringArray[saveCounter] = this.imageStringArray[i]; //adds the string to the new string array
                System.out.println(newImageStringArray[saveCounter]);
                saveCounter += 1; //adds 1 to the counter
            }
        } else { //if the image is flipped
            for (int i = bottomLineFoundWithOne; i > this.counterEmptyTop - valueToAdd; i--) { //runs through the imageStringArray from bottom to top
                this.newImageStringArray[saveCounter] = this.imageStringArray[i]; //adds the string to the new string array
                //System.out.println("saveCounter: " + saveCounter + " " + this.imageStringArray[i]);
                System.out.println(newImageStringArray[saveCounter]);
                saveCounter += 1; //adds 1 to the counter
            }
        }


        int newImageHeight; //holds the new height of the image
        if(counterEmptyTop == 0){ //if there is no empty lines from top
            newImageHeight = bottomLineFoundWithOne; //sets the new height to the bottom line with a '1' in it
        } else { //if there is empty lines from top
            newImageHeight = bottomLineFoundWithOne-this.counterEmptyTop+1; //sets the new height to the bottom line with a '1' in it + 1
        }

        System.out.println("newImageHeight: " + newImageHeight);
        System.out.println("ImageHeight: " + this.newImageStringArray.length);

        //removes 0 from left side if all lines contains 0 at same position in the beginning
        int counterEmptyLeft = 0; //counter for empty lines from left
        boolean lineWithOneFromLeft = false; //if a line with a '1' is found, then this is set to true

        for(int i = 0; i < this.imageWidth-1; i++){ //runs through the imageStringArray from left to right
            for(int j = 0; j < newImageHeight-1; j++){ //runs through the imageStringArray from top to bottom
                if(this.newImageStringArray[j].charAt(i) == '1'){ //if a '1' is found
                    lineWithOneFromLeft = true; //sets the boolean to true
                    break; //breaks the for loop
                }
            }
            if(lineWithOneFromLeft){ //if a '1' is found
                break;  //breaks the for loop
            } else { //if no '1' is found
                counterEmptyLeft += 1; //adds 1 to the counter for empty lines from left
            }
        }

        //removes 0 from right side if all lines contains 0 at same position in the beginning
        int counterEmptyRight = 0; //counter for empty lines from right
        boolean lineWithOneFromRight = false; //if a line with a '1' is found, then this is set to true

        for(int i = this.imageWidth-1; i > 0; i--){ //runs through the imageStringArray from right to left
            for(int j = 0; j < newImageHeight-1; j++){ //runs through the imageStringArray from top to bottom
                if(this.newImageStringArray[j].charAt(i) == '1'){ //if a '1' is found
                    lineWithOneFromRight = true; //sets the boolean to true
                    break; //breaks the for loop
                }
            }

            if(lineWithOneFromRight){ //if a '1' is found
                break; //breaks the for loop
            } else { //if no '1' is found
                counterEmptyRight += 1; //adds 1 to the counter for empty lines from right
            }
        }

        System.out.println("removed 0 from left: " + counterEmptyLeft);
        System.out.println("removed 0 from right: " + counterEmptyRight);


        //////saves to new string array without 0 lines
        int newStringLength = this.newImageStringArray[0].length()-counterEmptyLeft-counterEmptyRight; //holds the new length of the strings in the new string array

        this.finalStringArray = new String[newImageHeight]; //creates new string array

        for(int i = 0; i < newImageHeight; i++){ //runs through the new string array
            this.finalStringArray[i] = this.newImageStringArray[i].substring(counterEmptyLeft, this.imageWidth-counterEmptyRight); //adds the string to the new string array
            System.out.println(this.finalStringArray[i]); //prints the string
        }

        this.imageHeight = newImageHeight; //sets the new height of the image
        this.imageWidth = newStringLength; //sets the new width of the image
    }



    /**
     * Generate a string with data for the PLC to know what its about to receive.
     * here is an example:    "Data:H098,W098,S098,R2,Array:"
     * It says:    Data: height=098 , Width=098 , Rows=2 , And Array:
     */
    public void StringStartData(){
        String heightString = Integer.toString(this.finalStringArray.length); //holds the height of the image as a string
        String widthString = Integer.toString(this.imageWidth); //holds the width of the image as a string
        this.intSizeString = this.imageWidth; //sets the intSizeString to the imageWidth
        String sizeString = Integer.toString(this.intSizeString); //holds the size of the image as a string

        //Converts int value of width and height to a string and makes i 3 letter string. From 98 to "098"
        for(int i = 0; i < 3; i++) {
            if (heightString.length() < 3) { //if the string is under 3 letters long
                heightString = "0" + heightString;  //Adds a "0" in front of the string value
            }
            if (widthString.length() < 3) {  //if the string is under 3 letters long
                widthString = "0" + widthString;    //Adds a "0" in front of the string value
            }
            if (sizeString.length() < 3) {  //if the string is under 3 letters long
                sizeString = "0" + sizeString;    //Adds a "0" in front of the string value
            }
        }

        int counter = this.imageWidth;   //counter of how many characters the string is made of
        this.rows = 1;  //holds the found number of rows/string to send the PLC for each line
        while(counter > 80){    //Runs thought to find number of rows of max 80 letters to be sendt to the PLC
            counter -= 80;
            this.rows ++;
        }

        this.startData += "Data:H" + heightString + ",W" + widthString + ",S" + sizeString + ",R" + rows + ",Array:";
        System.out.println(this.startData); //prints to java terminal.
    }



    /**
     * Prints out the imageArray in the java terminal
     *
     * 10203  ->  1 | 0 | 2 | 0 | 3
     * 00000  ->  0 | 0 | 0 | 0 | 0
     * 12313   -> 1 | 2 | 3 | 1 | 3
     */
    public void printArray(){ //prints out the imageArray in the java terminal
        System.out.println(Arrays.deepToString(imageArray)
                .replace("],","\n").replace(",","\t")
                .replaceAll("[\\[\\]]", " "));
    }

    /**
     * Sends strings to PLC one row at a time.
     */
    public void sendDataToPLC() {
        int copySize; //to set the size to copy from string
        int sizeRemaining = 0; //remaining size left to copy from the imageStringArray
        String rowString = ""; // to save the copied out part

        robot.connect();  //opens up the TCP connection to the PLC
        try {
            Thread.sleep(1000); //1 second delay
        } catch (InterruptedException e) { //catches the exception
            Thread.currentThread().interrupt(); //interrupts the thread
        }
        robot.write(this.startData);  //writes our start message with important information of the string incoming
        System.out.println("Starts sending data to PLC");

        for(int i = 0; i < this.finalStringArray.length; i ++) {
            sizeRemaining = this.finalStringArray[i].length();
            for(int n = 0; n < this.rows; n++){
                if(sizeRemaining > 80){ //if sizeRemaining is still more than 80 characters long we need to cap it to 80 long still
                    sizeRemaining -= 80;
                    copySize = 80;  //maximum copy size for TCP string transmission

                }
                else{    //when the sizeRemaining is finally 80 or less
                    copySize = sizeRemaining; //we can just copy that amount out of the string
                }

                rowString = String.valueOf(this.finalStringArray[i].toCharArray(), n*80, copySize);  //cuts out from 1-80 characters from the imgString
                System.out.println(rowString);  //to see in the terminal what we are sending to the PLC

                try {
                    Thread.sleep(500);  //delay of 0,5 second
                } catch (InterruptedException e) { //catches the exception
                    Thread.currentThread().interrupt(); //interrupts the thread
                }
                robot.write(rowString);  //sends the cut-out string to the PLC
            }
        }

        try {
            Thread.sleep(1000); //1 second delay
        } catch (InterruptedException e) { //catches the exception
            Thread.currentThread().interrupt(); //interrupts the thread
        }
        robot.write("END");   //sends "END" to the PLC, witch we use to know when the data transmission is done
        System.out.println("Done sending data to PLC");
    }

    public static void main(String[] args) {
        System.out.println("Program Working");

        Main main = new Main(); //creates a new instance of the Main class

        //System.out.println("remove white's: " + main.removeWhiteEndFromString("100001111000"));

        main.runGetMagnitudeArray(); //runs the method to get the magnitude array
        System.out.println("Calculating please wait.|.|.|");
        main.convertToBinaray(); //converts the imageArray to a binary imageArray

        //main.printArray(); //prints out the imageArray in the java terminal

        main.convertToString(); //converts the imageArray to a string array
        main.StringStartData(); //creates the startData string
        main.sendDataToPLC(); //sends the data to the PLC
    }


}