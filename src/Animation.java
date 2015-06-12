
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import processing.core.*;
import processing.data.Table;
import processing.data.TableRow;

public class Animation extends PApplet {

	int width = 1025;//1000,1025
	int height = 1000;//1000
	
	PImage map;
	String mapFile;
	MercatorMap mercatorMap;

	Table tableFlows;
	String kindLocation = "city-all";//city-all,city,state,country
	Map<String, String> nodes = new Hashtable<String, String>();
	Map<Integer, List<Flow>> flows = new Hashtable<Integer, List<Flow>>();
	Map<Integer, Map<String, Integer>> points = new Hashtable<Integer, Map<String, Integer>>();

	int endYear = 2013;//2013
	int startYear = 2000;//2010, 2000, 1990, 1980, 1960
	int currentYear = startYear;
	
	int increment = 0;
	int flagSaveFrame = 0;
	int resolution = 0;

	boolean rotateMap = false;
	boolean cumulatePath = false;
	boolean animating = true;
	boolean drawFlows = false;
	boolean drawPoints = true;
	boolean drawNodes = true;
	boolean focusBrazil = true;

	public void setup() {
		if(focusBrazil)
			rotateMap = false;
		
		size(width, height, P3D );

		//"mapbox.streets.world.3.png"
		//"mapbox.light.world.1.png"
		//"mapbox.world.2.png"
		//"mapbox.comic.world.2.png"
		mapFile = "mapbox.streets.world.3.png";
		map = loadImage("data/ui/"+mapFile);
				
		mercatorMap = new MercatorMap(width, height, 85, -85, -180, 180);
		
		for (int year = startYear; year <= endYear; year++) {
			tableFlows = loadTable("data/flows/"+kindLocation+"/lattes-flows-"+kindLocation+"-"+year+".csv", "header");
			
			List<Flow> flowsByYear = new ArrayList<Flow>();
			Map<String, Integer> pointsByYear = new Hashtable<String, Integer>();
			
			for (TableRow row: tableFlows.rows()) {
				Flow flow = new Flow(
					row.getFloat("oX"),
					row.getFloat("oY"),
					row.getFloat("dX"),
					row.getFloat("dY"),
					row.getInt("trips"),
					mercatorMap,
					this
				);
				
				if(row.getString("cO").equals("brazil") && row.getString("cD").equals("brazil") && focusBrazil){
					flowsByYear.add(flow);
					
					int trips = row.getInt("trips");
					String key = flow.idBegin();
					if(pointsByYear.containsKey(key)){
						pointsByYear.put(key, pointsByYear.get(key)-trips);
					}else{
						pointsByYear.put(key, -trips);
					}

					key = flow.idEnd();
					if(pointsByYear.containsKey(key)){
						pointsByYear.put(key, pointsByYear.get(key)+trips);
					}else{
						pointsByYear.put(key, trips);
					}
				}if(!focusBrazil){
					flowsByYear.add(flow);
					
					int trips = row.getInt("trips");
					String key = flow.idBegin();
					if(pointsByYear.containsKey(key)){
						pointsByYear.put(key, pointsByYear.get(key)-trips);
					}else{
						pointsByYear.put(key, -trips);
					}

					key = flow.idEnd();
					if(pointsByYear.containsKey(key)){
						pointsByYear.put(key, pointsByYear.get(key)+trips);
					}else{
						pointsByYear.put(key, trips);
					}
				}
				
				
			}
			flows.put(year, flowsByYear);
			points.put(year, pointsByYear);
		}
	}

	public void draw() { 
		//light.world - (209,209,209)
		//world(), streets.world(210,209,211-d2d1d3) - (241,239,241)
		//comic.world - (0,71,109)
		background(241,239,241); 

		pushMatrix();
		if(focusBrazil){
			scale(4f);
			translate(-200, -400);
		}
		if(rotateMap){
			scale(0.75f);
			//translate(100, 250);
			//translate(100, 100);
			translate(130, 100);
			rotateX(radians(40));			
		}
		image(map, 0 ,0);
		if(animating){
			if(mapFile == "mapbox.streets.world.3.png")
				translate(-32, 0);
			drawFlows();
			//drawFlowsStepByStep();
		}
		popMatrix();
		
		if(animating){
			String content = "Year "+currentYear;
			pushMatrix();
			textSize(20);
			stroke(255,255,255);
			fill(255);
			rect(20, 5, textWidth(content)+25, 35);
			fill(0, 102, 153, 204);
			text(content, 45, 45, 30);// width/2-textWidth(content)/2, 45
			popMatrix();
			//flagSaveFrame++;
			//if(flagSaveFrame%3 == 0)
				saveFrame("data/frames/######.png");
		}
	}

	private void drawFlows() {
		Map<String, Integer> pointsExist = new Hashtable<String, Integer>();
		
		for(int year = startYear; year<=currentYear; year++){
			int sizeFlows = 0;
			
			for(Flow flow: flows.get(year)){
				sizeFlows += flows.get(year).size();

				//curves
				if(drawFlows){
					boolean exist = pointsExist.containsKey(flow.idBeginEnd()) || pointsExist.containsKey(flow.idEndBegin());
					if(!exist)
						pointsExist.put(flow.idBeginEnd(), 1);
					if(!exist && cumulatePath){
						flow.drawEllipse(width, height);
						//flow.drawCurve(100);
						//flow.drawBezier();
					}	
					if(year == currentYear && !exist &&  !cumulatePath){
						flow.drawEllipse(width, height);
						//flow.drawCurve(100);
						//flow.drawBezier();
					}
				}

				//points
				if(year == currentYear){
					if (increment == 1){
						delay(1000);
						resolution = sizeFlows*10;
					}
					if (increment<=resolution){
						increment++;
						if(drawPoints)
							if(focusBrazil)
								flow.drawPointsFlow(increment/(float)resolution, 0.1f);
							else
								flow.drawPointsFlow(increment/(float)resolution, 1);
					}
					if (increment == resolution){
						if(currentYear == endYear){
							animating = false;
						}
						if(currentYear != endYear){
							currentYear++;
						}
						increment = 0;
					}
				}

			}

			//nodes
			if(drawNodes){
				for(String point: points.get(currentYear).keySet()){
					pushMatrix();
					if(points.get(currentYear).get(point)<0){
						stroke(255,0,0);
						fill(255,0,0);
					}else{
						stroke(0,0,255);
						fill(0,0,255);
					}
					if(focusBrazil)
						ellipse(Float.parseFloat(point.split(",")[0]),Float.parseFloat(point.split(",")[1]),0.1f,0.1f);
					else
						ellipse(Float.parseFloat(point.split(",")[0]),Float.parseFloat(point.split(",")[1]),2,2);
					popMatrix();
				}
			}
		}
	}

	// animated steped curve 
	private void drawFlowsStepByStep() {
		
		for(Flow flow: flows.get(currentYear)){
			int sizeFlows = flows.get(currentYear).size();

			if (increment == 1){
				resolution = sizeFlows*10;//25,30,50
			}
			
			//curves
			int step = (int) (resolution*1);//3, 2, 0.8
			if (increment<=resolution+step){
				increment++;
				
				int endValue = (increment > resolution)? resolution : increment;
				int startValue = (increment-step)<0 ? 0 : increment-step; 
				flow.drawCurveAnimated(endValue, startValue, resolution);
			}
			
			//points
			if (increment<=resolution && drawPoints){ 
				if(focusBrazil)
					flow.drawPointsFlow(increment/(float)resolution, 0.1f);
				else
					flow.drawPointsFlow(increment/(float)resolution, 1);
			}
			
			if (increment == resolution+step){
				if(currentYear == endYear){
					animating = false;
				}
				if(currentYear != endYear){
					currentYear++;
				}
				increment = 0;
			}
		}
	}
	
}