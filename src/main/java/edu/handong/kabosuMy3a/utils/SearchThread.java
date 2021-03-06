package edu.handong.kabosuMy3a.utils ;

import java.util.*;
import java.net.*;
import java.io.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory ;

import edu.handong.kabosuMy3a.utils.datamodel.bookInfo ;
import edu.handong.kabosuMy3a.utils.useful.* ;

public class SearchThread implements Runnable{


	private static String clientID = "mzPPplh7Z4KjCfYoj6Ve";
	private static String clientSecret = "bjFwkbK7Ay";
	
	private ArrayList<bookInfo> infoList ; 
	private ArrayList<bookInfo> searchedInfo ;

	private String keyword ; 
	private int option = 0 ; 
	private int boxnumber = 0 ;


	public SearchThread(ArrayList<bookInfo> searchedInfo, String keyword, int option,int boxnumber){
		
		/*
		 * option 0 : default(ISBN)
		 * option 1 : Title Search
		 * option 2 : ISBN Search 
		 */
		this.option = option;
		this.keyword = keyword;
		this.boxnumber = boxnumber;
		this.searchedInfo = searchedInfo ;
		infoList = null ;
		
	}

	@Override
	public void run(){

		searching();	
		try{
			if(infoList.isEmpty()) throw new searchFailedException() ;			
		}catch(searchFailedException e){
			saveErrorLog(); 
			System.out.println(e.getMessage());
			return;
		}
		printSearchedList();
		if(option == 1){	
			returnInfoSearchedByTitle();
		}else{
			returnInfoSearchedByISBN();	
		}
		return ;
	}

	private void searching(){

		URL url ;
		int start = 0;
		final int MAX_DISPLAY = 10;
		URLConnection URLcon;
		try{
	    		if(option == 0 || option ==2){
				url = new URL("https://openapi.naver.com/v1/search/book_adv.xml"
						+"?d_isbn="+ URLEncoder.encode(keyword, "UTF-8"));
			}else{
				url = new URL("https://openapi.naver.com/v1/search/book.xml?query="
						+ URLEncoder.encode(keyword, "UTF-8")
						+ (MAX_DISPLAY != 0 ? "&display=" + MAX_DISPLAY : "") 
						+ (start != 0 ? "&start=" + start : ""));
			}
			URLcon = url.openConnection();
			URLcon.setRequestProperty("X-naver-Client-Id", clientID);
			URLcon.setRequestProperty("X-naver-Client-Secret", clientSecret);
				
			XmlPullParserFactory factory; 
           		factory = XmlPullParserFactory.newInstance();
            		XmlPullParser parser = factory.newPullParser();
			parser.setInput(new InputStreamReader(URLcon.getInputStream(),"UTF-8"));

			int eventType = parser.getEventType();
			bookInfo bI = null ;

			while(eventType != XmlPullParser.END_DOCUMENT){

				if (eventType == XmlPullParser.START_DOCUMENT){
					infoList = new ArrayList<bookInfo>();
				}
				else if(eventType == XmlPullParser.START_TAG){
					String tag = parser.getName();
					
					switch (tag){
					
					case "item" : 
						bI = new bookInfo();
						bI.setBoxNumber(boxnumber);
						break;
					
					case "title" :
						if(bI != null) bI.setTitle(parser.nextText());
						break;
					
					case "author" :
						if(bI != null) bI.setAuthor(parser.nextText());
						break;
					
					case "price" :
						if(bI != null) bI.setPrice(parser.nextText());
						break;

					case "publisher" :
						if(bI != null) bI.setPublisher(parser.nextText());
						break;
					
					case "isbn" :
						if(bI != null) bI.setISBN(parser.nextText());
						break;

					case "pubdate" :
						if(bI != null) bI.setPubDate(parser.nextText());
						break;
					}

				}
				else if(eventType == XmlPullParser.END_TAG){
					String tag = parser.getName();
					if (tag.equals("item")){
						infoList.add(bI);
						bI = null;
					}
				}
				
				eventType = parser.next();
			}

		 	URLcon.getInputStream().close();
		   	
	    	   }catch(XmlPullParserException e){
			   e.printStackTrace();
	    	   
		   }catch(Exception e){
			
			   e.printStackTrace();
		   }
	}
	
	private void printSearchedList(){
		
		int index  = 0; 

		for(bookInfo b : infoList){
			System.out.println(Integer.toString(++index));
			System.out.println(b.toString());
			System.out.println();	
		}
	}
	
	private void saveErrorLog(){

		ArrayList<String> errorlog = new ArrayList<String>() ;
		errorlog.add(keyword);
		Utils.writeAFile(errorlog,"errorlog.txt") ;

	}


	private void returnInfoSearchedByTitle(){
		
		bookInfo infoToReturn = null;

			if ( infoList.size() > 1){
				System.out.print("*-input <one number> you want to save ");
				System.out.println("if you don't want to save, input 0-*");
				Scanner keyboard = new Scanner(System.in);
				try{
			    		int num = Integer.parseInt(keyboard.nextLine());
					if(num !=0 && num <= infoList.size()){
			    			infoToReturn = infoList.get(num-1);
			    			System.out.print(Integer.toString(num)+" ");
					}
				}catch(Exception e){
			    	System.out.println("**save failed  ");
				}
			
			}else infoToReturn = infoList.get(0);

			synchronized(searchedInfo){
				if (infoToReturn != null){			
					searchedInfo.add(infoToReturn);
					System.out.println("saved successfully");
				}	
			}

	}

	private void returnInfoSearchedByISBN(){

		int resultNum = 0;
		synchronized(searchedInfo){
			for(bookInfo bI : infoList){
				searchedInfo.add(bI);
				resultNum ++;
				System.out.println((resultNum > 1
							?Integer.toString(resultNum)+" "
							:"")+"saved successfully"); 
			}
		}
	}
}
