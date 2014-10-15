package org.mars.proxybase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Rule {
	private String fire=null;
	private String port=null;
	private String host=null;
	private String url=null;
	private String regExp = null; 	// It is computed when setFire is called. 
									// It remains null if parse errors exist
	private List<Elem> elems = null;
	
	public static void main(String[] args) {
		System.out.println("Testing paramsToStar");
		Rule rule = new Rule();
		try {
			System.out.println(rule.paramsToStar("/pepe/{PORT}/hola/{CACA}/{ULL}/hola"));
			System.out.println(rule.paramsToStar("{PORT}/hola"));
			System.out.println(rule.paramsToStar("/pepe/{PORT}"));
			System.out.println(rule.paramsToStar("/pepe/hola"));
			rule.setFire("{HOST}/pepe/{PORT}/hola/{CACA}/{ULL}/hola");
			System.out.println(rule.isExecuted("/pepe/1111/hola/qqq/ppp/hola"));
			System.out.println(rule.isExecuted("/pepe"));
			System.out.println(rule.isExecuted("/pepe/hola/1111/qqq/ppp/hola"));
			System.out.println(rule.isExecuted(""));
			System.out.println(rule.isExecuted("sddsddfsdfsdfsdfsdf"));
			System.out.println(rule.isExecuted("/pepe//hola///hola"));
			
			rule.setPort("{PORT}");
			rule.setHost("{HOST}:{PORT}");
			rule.setUrl("/pepe/{PORT}/{ULL}/hola/{CACA}");
			Appliance a = rule.applyRule("127.0.0.1/pepe/8080/hola/ENDPOINT/MIDPOINT/hola");
			Map<String, String> map = a.getMap();
			for(String key:map.keySet()) {
				System.out.println(key + " : " + map.get(key));
			}
			System.out.println("HOST: " + a.getHost());
			System.out.println("PORT: " + a.getPort());
			System.out.println("URL: " + a.getUrl());
			/*
			for (Elem e:rule.elems) {
				System.out.println(e.getKey()+ ", " + e.getIndex() + ", " + e.getLastChar());
			}
			
			rule.setFire("{PORT}/hola");
			for (Elem e:rule.elems) {
				System.out.println(e.getKey()+ ", " + e.getIndex() + ", " + e.getLastChar());
			}
			
			rule.setFire("hola/{PORT}");
			for (Elem e:rule.elems) {
				if (e.getLastChar()==null) { System.out.println("Ohh yes"); }
				System.out.println(e.getKey()+ ", " + e.getIndex());
			}
			
			rule.setFire("hola/");
			for (Elem e:rule.elems) {
				System.out.println(e.getKey()+ ", " + e.getIndex());
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Return null if the rule does not apply to the given input, an Appliance if it does
	 * @param input
	 * @return
	 */
	public Appliance applyRule(String input) {
		if (!isExecuted(input)) return null;
		Appliance out = new Appliance();
		Map<String,String> map = new HashMap<String, String>();
		if (this.elems.size()==0) {
			out.setHost(this.getHost());
			out.setPort(this.getPortInteger());
			out.setUrl(this.getUrl());
		} else {
			String hostApp = this.getHost();
			String portApp = this.getPort();
			String urlApp = this.getUrl();
			int pivotReal = 0;
			int pivotRegExp = 0;
			for (Elem e: elems) {
				int i=this.fire.indexOf(e.getKey(), pivotRegExp);
				int offset = i - pivotRegExp;
				int realOffset = pivotReal+offset;
				String value = null;
				if (e.getLastChar()==null) {
					// Here we will be at the of the elements!
					value = input.substring(realOffset);
				} else {
					int j = input.indexOf(e.getLastChar(), realOffset);
					value = input.substring(realOffset, j);
					pivotReal = j;
					pivotRegExp = i + e.getKey().length();
				}
				map.put(e.getKey(), value);
				hostApp = hostApp.replace(e.getKey(), value);
				portApp = portApp.replace(e.getKey(), value);
				urlApp = urlApp.replace(e.getKey(), value);
			}
			out.setMap(map);
			out.setHost(hostApp);
			out.setPort(Integer.parseInt(portApp));
			out.setUrl(urlApp);
		}
		return out;
	}
	
	
	private boolean isExecuted(String input) {
		if (fire==null) return false;
		if (fire.length()==0) return true;
		if (input==null) return false;
		if (input.length()==0) return false;
		if (regExp == null) return false;
		
		return input.matches(regExp);
	}
	
	private String paramsToStar(String in) throws Exception {
		String out="";
		int pointer=0;
		in = in.replace("*", ".*");
		int i = in.indexOf('{', pointer);
		while (i!=-1) {
			out += in.substring(pointer, i);
			out += ".*";
			int j = in.indexOf('}',pointer);
			if (j==-1 || i > j) {
				throw new Exception("Parse error: Mismatch between { and }");
			}
			
			pointer = j+1;
			i = in.indexOf('{',  pointer);
		}
		out += in.substring(pointer);
		return out;
	}
	
	private void uploadElements() {
		this.elems = new ArrayList<Elem>();
		int pivot = 0;
		
		Elem e = getElementInfo(pivot, this.fire);
		while(e!=null) {
			this.elems.add(e);
			pivot = e.getKey().length() + e.getIndex();
			e = getElementInfo(pivot, this.fire);
		}
	}
	
	private Elem getElementInfo(int pivot, String str) {
		Elem e = null;
		int i = str.indexOf("{", pivot);
		if (i==-1) return null;
		int j = str.indexOf("}", pivot);
		if (j==-1 || i>j) return null;
		e = new Elem();
		e.setIndex(i);
		e.setKey(str.substring(i,j+1));
		int lastCharindex = j+1;
		if (lastCharindex>=str.length()) {	// We are at the end of line
			e.setLastChar(null);
		} else {
			e.setLastChar("" + str.charAt(lastCharindex));
		}
			
		return e;
	}
	
	// Getters and Setters
	public String getFire() {
		return fire;
	}
	public void setFire(String fire) {
		this.fire = fire;
		try {
			this.regExp = this.paramsToStar(fire);
			uploadElements();
		} catch (Exception e) {
			this.regExp = null;
			e.printStackTrace();
		}
	}
	public String getPort() {
		return port;
	}
	
	public int getPortInteger() {
		try {
			return Integer.parseInt(this.port);
		} catch (Exception e) {
			return -1;
		}
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getExpReg() {
		return this.regExp;
	}
	
	private static class Elem {
		private String key;
		private int index;
		private String lastChar;
		
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public String getLastChar() {
			return lastChar;
		}
		public void setLastChar(String lastChar) {
			this.lastChar = lastChar;
		}
	}
	
	private static class Appliance {
		private int port;
		private String host;
		private String url;
		private Map<String, String> map = new HashMap<String,String>();
		
		public void setMap(Map<String, String> map) {
			this.map = map;
		}
		
		public Map<String, String> getMap() {
			return this.map;
		}
		
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		
	}
}