package org.mars.proxybase;

import java.util.HashMap;
import java.util.Map;

public class Rule {
	private String fire=null;
	private String port=null;
	private String host=null;
	private String url=null;
	private String regExp = null; 	// It is computed when setFire is called. 
									// It remains null if parse errors exists.
	
	public static void main(String[] args) {
		System.out.println("Testing paramsToStar");
		Rule rule = new Rule();
		try {
			System.out.println(rule.paramsToStar("/pepe/{PORT}/hola/{CACA}/{ULL}/hola"));
			System.out.println(rule.paramsToStar("{PORT}/hola"));
			System.out.println(rule.paramsToStar("/pepe/{PORT}"));
			System.out.println(rule.paramsToStar("/pepe/hola"));
			rule.setFire("/pepe/{PORT}/hola/{CACA}/{ULL}/hola");
			System.out.println(rule.isExecuted("/pepe/1111/hola/qqq/ppp/hola"));
			System.out.println(rule.isExecuted("/pepe"));
			System.out.println(rule.isExecuted("/pepe/hola/1111/qqq/ppp/hola"));
			System.out.println(rule.isExecuted(""));
			System.out.println(rule.isExecuted("sddsddfsdfsdfsdfsdf"));
			System.out.println(rule.isExecuted("/pepe//hola///hola"));
			
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
	
	// Getters and Setters
	public String getFire() {
		return fire;
	}
	public void setFire(String fire) {
		this.fire = fire;
		try {
			this.regExp = this.paramsToStar(fire);
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
	
	private static class Appliance {
		private int port;
		private String host;
		private String url;
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