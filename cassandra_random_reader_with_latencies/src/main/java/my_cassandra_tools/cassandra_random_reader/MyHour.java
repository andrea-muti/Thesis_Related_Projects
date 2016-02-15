package my_cassandra_tools.cassandra_random_reader;


import java.util.*;

  public class MyHour{

	  public int h,m,s,ms;
	  
	  MyHour(){
		  
	  }
	  
	  public void setCurrentMoment(){
		  
		  Calendar calendar = new GregorianCalendar();
		  
  		  this.h = calendar.get(Calendar.HOUR_OF_DAY);
  		  this.m = calendar.get(Calendar.MINUTE);
  		  this.s = calendar.get(Calendar.SECOND);
  		  this.ms= calendar.get(Calendar.MILLISECOND);
  		
	  }
	  
	  public String toString(){
		  String string=String.valueOf(h)+":"+String.valueOf(m)+":"+String.valueOf(s)+":"+String.valueOf(ms);
		  return string;
	  }
	  
	  
 
}