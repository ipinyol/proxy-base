package org.mars.proxybase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class that processes the rules
 * @author ipinyol
 *
 */
public class RuleEngine {
	List<Rule> rules = new ArrayList<Rule>();
	
	public static void main(String []args) {
	    Properties prop = ProxyBase.readProperties();
	    RuleEngine ruleEngine = new RuleEngine(prop);
	    for(Rule rule: ruleEngine.getRules()) {
	        System.out.println("----Rule----");
	        System.out.println("Fire: " + rule.getFire());
	        System.out.println("Host: " + rule.getHost());
	        System.out.println("Port: " + rule.getPort());
	        System.out.println("URL: " + rule.getUrl());
	        System.out.println("RegExp: " + rule.getExpReg());
	    }
	    
	    Rule.Appliance app = ruleEngine.applyRule("/iMathConnect/getUserId/112233232");
	    System.out.println("----Appliance----");
	    System.out.println(app.getUrl());
	    System.out.println(app.getHost());
	    System.out.println(app.getPort());
	    
	    app = ruleEngine.applyRule("/iMathTest/8889/127.3.5.6/execShell");
	    System.out.println("----Appliance----");
	    System.out.println("/iMathTest/8889/127.3.5.6/execShell");
        System.out.println(app.getUrl());
        System.out.println(app.getHost());
        System.out.println(app.getPort());
	    
	}
	
	public static String IGNORE_LINE = "--";
	RuleEngine(Properties prop) {
	    InputStream is = RuleEngine.class.getClassLoader().getResourceAsStream("org/mars/proxybase/rules.config");
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String str;
	    try {
	        int lines = 0;
	        while ((str = br.readLine()) != null) {
	            lines++;
	            if (str!=null) {
	                if (str.length()>=2) { // Check the length of the line is higher than 1
	                    if (!str.substring(0,2).equals(IGNORE_LINE)) { // Check that the line does not start with --
	                        String [] parts = str.split(",");
	                        if (parts.length<2) {  // Check that we have at least two parameters
	                            throw new Exception ("Malformed rule in line " + lines + ". At least 2 fileds must be infrormed");
	                        }
	                        Rule rule = updateRuleValues(parts, prop);
	                        rules.add(rule);
	                    }
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            br.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	public Rule.Appliance applyRule(String input) {
	    Rule.Appliance app = null;
	    int i = 0;
	    while (app==null && i < this.getRules().size()) {
	        Rule rule = this.rules.get(i);
	        app = rule.applyRule(input);
	        i++;
	    }
	    return app;
	}
	
	public List<Rule> getRules() {
	    return this.rules;
	}
	
	private Rule updateRuleValues(String [] parts, Properties prop) {
	    Rule rule = new Rule();
	    // We update the fire property 
        rule.setFire(parts[0].trim());
        
        // We update the Port property
        if (parts[1].length()==0) {
            rule.setPort(prop.getProperty(ProxyBase.DEFAULT_PORT_OUT));
        } else {
            rule.setPort(parts[1].trim());
        }
        
        // We update the Host property
        if (parts.length>=3) {
            if (parts[2].length()==0) {
                rule.setHost(prop.getProperty(ProxyBase.DEFAULT_HOST));
            } else {
                rule.setHost(parts[2].trim());
            }
        } else {
            rule.setHost(prop.getProperty(ProxyBase.DEFAULT_HOST));
        }
        
        // We update the URL property
        if (parts.length==4) {
            rule.setUrl(parts[3].trim());
        }
        return rule;
	}
}


