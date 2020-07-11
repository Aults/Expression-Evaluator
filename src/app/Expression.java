package app;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {
	public static String delims = " \t*+-/()[]";
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
    public static void makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	/** DO NOT create new vars and arrays - they are already created before being sent in
    	 ** to this method - you just need to fill them in.
    	 **/
       	String delimswBrack = " .\t*+-/()[]1234567890"; // Any valid characters for an equation
        expr = expr.replaceAll("\\s", ""); //Removes spaces
    	StringTokenizer split = new StringTokenizer(expr, delimswBrack);
    	while (split.hasMoreTokens()) {
    		String token = split.nextToken();
			String exprT = expr;
	    	boolean passed = false;
    		while(!passed || exprT == "") { //Have we hit another operator? Is the string empty?
    			int i = exprT.indexOf(token);
    			if(i == -1) break; // Does not exist in string
    			if((i == 0 || (exprT.length()>=1 && delimswBrack.indexOf(exprT.charAt(i-1)) != -1))
    				&& (exprT.length() > i+1+token.length() && exprT.charAt(i+token.length()) == '[')) {
    					passed = true;
    			} else {
    				exprT = exprT.substring(i+1); 
    			}
    		}
    		if(passed) { // We have found an array
    			if(!arrays.contains(new Array(token))) {
    				arrays.add(new Array(token)); continue;
    			}
    		} else { // We have found a variable
    			if(!vars.contains(new Variable(token))) {
    				vars.add(new Variable(token)); continue;
    			}
    		}
    		
    	}
    }
    
    /**
     * Loads values for variables and arrays in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var); // Index of variable
            int arri = arrays.indexOf(arr); // Index of array
            if (vari == -1 && arri == -1) { // Does not exist, so it's not in the string and can be passed over
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
	public static int parenMatch(String expr, char targ) {
		Stack<Character> stk = new Stack<>();
		int counter = -1;
		do {
			char ch = expr.charAt(counter+1);
			if (ch == targ) { //Did we find the target?
				stk.push(ch); //Add it to the stack, then move on
				counter++;
				continue;
			}
			char opp = 0;
			if(targ == '(') 
				opp = ')';
			else
				opp = ']';
			if (ch == opp) { // We found an opposite match, remove it from the stack
				stk.pop();
			}
			counter++;
		} while(!stk.isEmpty()); // When the stack is empty, we have found the matched parenthesis index, held in counter
		return counter;
	}
	public static String replaceVariables(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
		String modified = "";
		String name = "";
    	String numsDelims = "1234567890.\\t*+-/()"; // Valid characters
      	int endIndex;
    	int solveIndex;
    	float found = 0;
		for(int i = 0; i < expr.length(); i++) {
			char ch = expr.charAt(i);
			if(numsDelims.indexOf(ch) != -1) { // Is this a valid character?
				if(name != "") { // Have we just found a new operand, and previously held a variable?
					int temp = vars.indexOf(new Variable(name));
					found = vars.get(temp).value;
					modified += found;
					name = "";
				}
				modified += ch; 
				continue;
			}
			if(ch == '[') { 
				endIndex = i + parenMatch(expr.substring(i), '['); // Find respective bracket index
				solveIndex = (int)evaluate(expr.substring(i+1,endIndex), vars, arrays); // Evaluate inside expression
				int temp = arrays.indexOf(new Array(name));
				i = endIndex;
				found = arrays.get(temp).values[solveIndex];
				modified = modified + found;
				name = "";
				continue;
			}
			if(ch == ']') continue;
			name += ch;
		}
		if(name != "") { // Is there a remaining variable we haven't dealt with?
			int temp = vars.indexOf(new Variable(name));
			found = vars.get(temp).value;
			modified += found;
			name = "";
		}
		return modified;
	}
	
	private static String fixNeg(String expr) {
		String prevTerm = "";
		int begin = -1;
		if(expr.charAt(0) == '+') { // Invalid
			expr = expr.substring(1);
		}
		for(int i = 0; i < expr.length() - 1; i++) {
			if(Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.') { //Have we found a number?
				if(begin == -1) {
					begin = i;
				}
				prevTerm += expr.charAt(i);
			} else { // Deals with operands that would break evaluate, so we simplify them
				if(expr.substring(i,i+2).equals("+-")) {
					expr = expr.substring(0,i) + "-" + expr.substring(i+2);
				} else if(expr.substring(i,i+2).equals("--")) {
					expr = expr.substring(0,i) + "+" + expr.substring(i+2);
				} else if(expr.substring(i,i+2).equals("++")) {
					expr = expr.substring(0,i) + "+" + expr.substring(i+2);
				} else if(expr.substring(i,i+2).equals("*-")) {
					expr = expr.substring(0,begin) + "-" + prevTerm + "*" + expr.substring(i+2);
					expr = fixNeg(expr);
				} else if(expr.substring(i,i+2).equals("/-")) {
					expr = expr.substring(0,begin) + "-" + prevTerm + "/" + expr.substring(i+2);
					expr = fixNeg(expr);
				}
				begin = -1;
				prevTerm = "";
			}
		}
		return expr;
	}
    /**
     * Evaluates the expression.
     * 
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	String nums = "1234567890.";
    	float partialSum = 0;
        expr = expr.replaceAll("\\s", "");
        if(expr.length() == 0) return 0;
        expr = replaceVariables(expr, vars, arrays); // Replace variable names with their respective digits
        expr = fixNeg(expr); // Reduce any negative/positive combinations
        if(expr.length() == 1) {
        	if(nums.indexOf(expr.charAt(0)) == -1) return 0; // 
        	return Float.valueOf(expr.charAt(0) - '0');
        }
        int begin = -1;
        int end = -1;
        while(expr.indexOf("(") != -1 || expr.indexOf("[") != -1) { // Removes any parenthesis inside expression, using recursion
        	char targ = 0;
        	int brack = expr.indexOf("["); int parenthesis = expr.indexOf("(");
        	if(brack != -1 && parenthesis != -1) {
        		if(brack > parenthesis) {
        			begin = parenthesis;
        			targ = '('; 
        		} else {
        			begin = brack;
        			targ = '[';
        		}
        	} else if (brack == -1){
        		begin = parenthesis;
    			targ = '('; 
        	} else {
        		begin = brack;
    			targ = '[';
        	}
        	end = begin + parenMatch(expr.substring(begin), targ);
	        if(begin != -1) {
	        	partialSum = evaluate(expr.substring(begin+1,end),vars, arrays);
	        	expr = expr.substring(0,begin) + partialSum + expr.substring(end+1);
	        	expr = fixNeg(expr);
	        	partialSum = 0;
		        begin = -1;
		        end = -1;
	        } else break;
        }
    	partialSum = 0; begin = -1; end = -1; // Reset variables to reduce amount of memory
        String firstNum = "", secondNum = ""; char operator = 0; 
        while(true) { // Deals with '+' and '-' operands
        	expr = fixNeg(expr);
        	for(int i = 0; i < expr.length(); i++) {
        		if(i==0 && expr.charAt(0) == '-') {
        			begin = i;
        			firstNum += "-";
        		} else if(((expr.charAt(i) == '+' || expr.charAt(i) == '-'))) {
        			if(secondNum != "") {
        				break;
        			} else {
        				firstNum = ""; operator = 0; secondNum = "";
        			}
        		}else if((nums.indexOf(expr.charAt(i))!=-1) && operator == 0) {
	        		if(firstNum == "") begin = i;
	        		firstNum += expr.charAt(i);
	        	} else if(nums.indexOf(expr.charAt(i))!=-1 && operator != 0) {
	        		secondNum += expr.charAt(i);
	        		end = i;
	        	} else if((expr.charAt(i) == '*' || expr.charAt(i) == '/') && secondNum == ""){
	        		operator += expr.charAt(i);
	        	} else {
	        		break;
	        	}
        	}
        	switch(operator) { // Which operator are we dealing with?
        		case '*':
        			partialSum = Float.valueOf(firstNum) * Float.valueOf(secondNum);
        			break;
        		case '/':
        			partialSum = Float.valueOf(firstNum) / Float.valueOf(secondNum);
        			break;
        	}

        	if(begin != -1 && end != -1) {
	        	expr = expr.substring(0,begin) + partialSum + expr.substring(end+1);
        	} else {
        		break;
        	}
        	firstNum = ""; secondNum = ""; operator = 0; begin = -1; end = -1; partialSum = 0;
        }
        firstNum = ""; secondNum = ""; operator = 0;
        while(true) { // Deals with '*' and '/' operators
        	expr = fixNeg(expr);
        	for(int i = 0; i < expr.length(); i++) {
            	if(expr.charAt(i) == '-' && firstNum == "") {
            		firstNum = "-"; begin = i;
            	}else if(((expr.charAt(i) == '*' || expr.charAt(i) == '/'))) {
        			if(secondNum != "") {
        				break;
        			} else {
        				firstNum = ""; operator = 0; secondNum = "";
        			}
        		}else if((nums.indexOf(expr.charAt(i))!=-1) && operator == 0) {
	        		if(firstNum == "") begin = i;
	        		firstNum += expr.charAt(i);
	        	} else if(nums.indexOf(expr.charAt(i))!=-1 && operator != 0) {
	        		secondNum += expr.charAt(i);
	        		end = i;
	        	} else if((expr.charAt(i) == '+' || expr.charAt(i) == '-') && secondNum == ""){
	        		operator += expr.charAt(i);
	        	} else {
	        		break;
	        	}
        	}
        	switch(operator) {
        		case '+':
        			partialSum = Float.valueOf(firstNum) + Float.valueOf(secondNum);
        			break;
        		case '-':
        			partialSum = Float.valueOf(firstNum) - Float.valueOf(secondNum);
        			break;
        	}

        	if(begin != -1 && end != -1) {
	        	expr = expr.substring(0,begin) + partialSum + expr.substring(end+1);
        	} else {
        		break;
        	}
        	firstNum = ""; secondNum = ""; operator = 0; begin = -1; end = -1; partialSum = 0;
        }
    	expr = fixNeg(expr);
        return Float.valueOf(expr);
    }
}
