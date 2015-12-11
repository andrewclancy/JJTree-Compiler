import java.util.*;

@SuppressWarnings("unchecked")
public class SemanticCheckVisitor implements BasicLVisitor {


    private final static String GLOBAL_SCOPE = "GLOBAL_SCOPE";
    private static final LinkedHashSet<String> calledFunctions = new LinkedHashSet<>();
    private static final LinkedHashSet<String> declaredFunctions = new LinkedHashSet<>();
    private static final HashMap<String, HashMap<String, STC>> symbolTable = new HashMap<>();
    private static String oldScope = GLOBAL_SCOPE;
    private static String scope = GLOBAL_SCOPE;
    private static int scopeCounter = 0;
	
	@Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
	
@Override
    public Object visit(ASTprogram node, Object data) {

        symbolTable.put(scope, new HashMap<String, STC>());

        node.childrenAccept(this, data);

        if (declaredFunctions.size() == calledFunctions.size()) {
            System.out.println("All functions were called");
        } else {
            System.out.println("All functions were not called");
        }

        for (String scope : symbolTable.keySet()) {
            HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
            for (String key : scopedSymbolTable.keySet()) {
                STC symbolTableChild = scopedSymbolTable.get(key);
                if (symbolTableChild.getKind() != DataType.FUNCTION) {
                    if (symbolTableChild.getData("written") == null && symbolTableChild.getData("readFrom") == null) {
                        System.out.println(symbolTableChild.getIdentifier() + " in scope " + symbolTableChild.getScope() + " is not written to or read from");
                    } else if (symbolTableChild.getData("written") == null && ((boolean)symbolTableChild.getData("readFrom")) == true) {
                        System.out.println(symbolTableChild.getIdentifier() + " in scope " + symbolTableChild.getScope() + " is not written to but is read from");
                    } else if ((boolean) symbolTableChild.getData("written") == true && symbolTableChild.getData("readFrom") == null) {
                        System.out.println(symbolTableChild.getIdentifier() + " in scope " + symbolTableChild.getScope() + " is written but is not read from");
                    } else if ((boolean) symbolTableChild.getData("written") == true && (boolean)symbolTableChild.getData("readFrom") == true) {
                        System.out.println(symbolTableChild.getIdentifier() + " in scope " + symbolTableChild.getScope() + " is written and read from");
                    }
                }
            }
        }

        System.out.println();
        System.out.println("Symbol Table:");
		System.out.println("----------------------------------------------------------------------");
        System.out.printf("%-10s%-15s%-15s%-15s%-15s%n"," ", "Kind", "Type", "Identifier", "Scope", "Data");
		System.out.println("----------------------------------------------------------------------");
        for (String scope : symbolTable.keySet()) {
            HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
            for (String key : scopedSymbolTable.keySet()) {
                STC symbolTableChild = scopedSymbolTable.get(key);
                System.out.printf("%-10s%-15s%-15s%-15s%-15s%n", " ", symbolTableChild.getKind(), symbolTableChild.getType(), symbolTableChild.getIdentifier(), symbolTableChild.getScope(), symbolTableChild.getData());
            }
        }
		System.out.println("----------------------------------------------------------------------");

        return null;
    }

	
	@Override
    public Object visit(ASTVar node, Object data) {

        List<Token> identList = (List<Token>) node.jjtGetChild(0).jjtAccept(this, data);
        Token type = (Token) node.jjtGetChild(1).jjtAccept(this, data);

        for (Token identifier : identList) {
            HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
            if (scopedSymbolTable == null) scopedSymbolTable = new HashMap<>();

            STC variable = new STC(identifier, type, scope, DataType.VAR);

            if (scopedSymbolTable.get(identifier.image) != null) {
               System.out.println("Error: Identifier " + identifier.image + " already declared in " + scope);
            } else {
                scopedSymbolTable.put(identifier.image, variable);
                symbolTable.put(scope, scopedSymbolTable);
            }
        }

        return null;
    }
	
	 @Override
    public Object visit(ASTConstDecl node, Object data) {
        HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
        if(scopedSymbolTable == null) scopedSymbolTable = new HashMap<String, STC>();

        for (int i = 0; i < node.jjtGetNumChildren(); i=i+3) {
			
        	Token identifier = (Token) node.jjtGetChild(i).jjtAccept(this, data);
        	Token type = (Token) node.jjtGetChild(i+1).jjtAccept(this, data);
			
        	List<Token> assignment;
            if (node.jjtGetChild(i).jjtAccept(this, data) instanceof Token) {
                assignment = new ArrayList<>();
                assignment.add((Token) node.jjtGetChild(i).jjtAccept(this, data));
            } else {
                assignment = ((List<Token>) node.jjtGetChild(i).jjtAccept(this, data));
            }
            STC variable = new STC(identifier, type, scope, DataType.CONST);
            variable.addData("value", assignment);

	        if (scopedSymbolTable.get(identifier.image) != null) {
	            System.out.println("Error: Identifier " + identifier.image + " already declared in " + scope);
	        } else {
	            scopedSymbolTable.put(identifier.image, variable);
	            symbolTable.put(scope, scopedSymbolTable);
	        }

        }

        return null;
    }
	
	@Override
    public Object visit(ASTFunction node, Object data) {
        Token type = (Token) node.jjtGetChild(0).jjtAccept(this, data);
        Token identifier = (Token) ((SimpleNode) node.jjtGetChild(1)).jjtGetValue();
        List<Token> paramList = (List<Token>) node.jjtGetChild(1).jjtAccept(this, data);
        Node functionBodyNode = node.jjtGetChild(1);

        STC function = new STC(identifier, type, scope, DataType.FUNCTION);
        function.addData("paramList", paramList);

        HashMap<String, STC> globalSymbolTable = symbolTable.get(scope);



        oldScope = scope;
        scope = "function- " + ++scopeCounter;

        for (int i = 0; i < paramList.size() - 1; ) {
            HashMap<String, STC> functionSymbolTable = symbolTable.get(scope);
            Token paramIdentifier = paramList.get(i);
            Token paramType = paramList.get(++i);
            STC scopedVariable = new STC(paramIdentifier, paramType, scope, DataType.VAR);
            if (functionSymbolTable == null) functionSymbolTable = new HashMap<>();

            if (functionSymbolTable.get(paramIdentifier.image) != null) {
                System.out.println("Error: Identifier " + paramType.image + " already declared in " + scope);
            } else {
                functionSymbolTable.put(paramIdentifier.image, scopedVariable);
                symbolTable.put(scope, functionSymbolTable);
            }
        }

        functionBodyNode.jjtAccept(this, data);

        return null;
    }
	
	@Override
    public Object visit(ASTParamList node, Object data) {
        List<Token> paramList = new ArrayList();

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            paramList.add((Token) ((SimpleNode) node.jjtGetChild(i)).jjtGetValue());
        }

        return paramList;
    }
	
	 @Override
    public Object visit(ASTType node, Object data) {
        return node.jjtGetValue();
    }
	
	@Override
    public Object visit(ASTMainProg node, Object data) {
        scope = "main";
        node.childrenAccept(this, data);
        return null;
    }
	
	@Override
    public Object visit(ASTAssignment node, Object data) {
Token identifier = (Token) node.jjtGetChild(0).jjtAccept(this, data);

        List<Token> assignment = null;

        Object obj = node.jjtGetChild(1).jjtAccept(this, data);
        if (obj instanceof Token) {
        	assignment = new ArrayList<>();
            assignment.add((Token) obj);
        } else {
            assignment = (List<Token>) obj;
        }

        HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);

        STC scopedVariable = scopedSymbolTable.get(identifier.image);
        if (scopedVariable != null) {
            scopedVariable.addData("written", true);
            scopedVariable.addData("value", assignment);

            if(scopedVariable.getData("value") != null) {
                checkType(scopedVariable, scopedVariable.getData("value"));
            }
        }

        HashMap<String, STC> globalSymbolTable = symbolTable.get(GLOBAL_SCOPE);

        STC globalVariable = globalSymbolTable.get(identifier.image);
        if (globalVariable != null) {
            globalVariable.addData("written", true);
            globalVariable.addData("value", assignment);

            if(globalVariable.getData("value") != null) {
                checkType(globalVariable, globalVariable.getData("value"));
            }
        }


        return null;
    }
	
	 private void checkType(STC scopedVariable, Object values) {
        HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
        HashMap<String, STC> globalSymbolTable = symbolTable.get(GLOBAL_SCOPE);
        List<Object> valuesList = (List) values;

        for(Object obj : valuesList) {
            if(obj instanceof Token) {
                Token token = (Token) obj;

                if(token.kind == BasicLConstants.ID) {
                    STC resolved = null;
                    if(scopedSymbolTable.get(token.image) != null) {
                        resolved = scopedSymbolTable.get(token.image);
                    } else if(globalSymbolTable.get(token.image) != null) {
                        resolved = globalSymbolTable.get(token.image);
                    }
                    if(resolved != null) {
                        if(resolved.getType().kind != scopedVariable.getType().kind) {
                            System.out.println("Error: Right hand side of " + scopedVariable.getIdentifier() + " does not match type " + scopedVariable.getType());
                            return;
                        }
                    }
                } else {
                    if(token.kind != scopedVariable.getType().kind) {
                        System.out.println("Error: Right hand side of " + scopedVariable.getIdentifier() + " does not match type " + scopedVariable.getType());
                        return;
                    }
                }
            } else {
                checkType(scopedVariable, obj);
            }
        }
    }
	
	@Override
    public Object visit(ASTFunctionCall node, Object data) {

        node.childrenAccept(this, data);
        return null;
    }
	
	
    @Override
    public Object visit(ASTCondition node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
	
	@Override
    public Object visit(ASTIdentList node, Object data) {
        List<Token> identList = new ArrayList();

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            identList.add((Token) ((SimpleNode) node.jjtGetChild(i)).jjtGetValue());
			//System.out.println(((SimpleNode) node.jjtGetChild(i)).jjtGetValue());
        }
		
        return identList;
    }
	
	@Override
    public Object visit(ASTNumber node, Object data) {

        node.childrenAccept(this, data);
        return null;

    }
	
	 @Override
    public Object visit(ASTBoolean node, Object data) {
        Token value = (Token) node.jjtGetValue();
        value.kind = BasicLConstants.BOOL;
        return value;
    }
	
	@Override
    public Object visit(ASTID node, Object data) {

        Token identifier = (Token) node.jjtGetValue();

        HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
        HashMap<String, STC> globalSymbolTable = symbolTable.get(GLOBAL_SCOPE);

        if (scopedSymbolTable != null) {
            if (scopedSymbolTable.get(identifier.image) != null) {
                STC scopedVariable = scopedSymbolTable.get(identifier.image);
                scopedVariable.addData("readFrom", true);
                scopedSymbolTable.put(identifier.image, scopedVariable);
            }
        }

        if (globalSymbolTable != null) {
            if (globalSymbolTable.get(identifier.image) != null) {
                STC globalVariable = globalSymbolTable.get(identifier.image);
                globalVariable.addData("readFrom", true);
                globalSymbolTable.put(identifier.image, globalVariable);
            }
        }
        return node.jjtGetValue();
    }

	@Override
    public Object visit(ASTArgList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
	
	@Override
    public Object visit(ASTBinaryOp node, Object data) {
		Token binaryOp = (Token) node.jjtGetValue();
		
		HashMap<String, STC> scopedSymbolTable = symbolTable.get(scope);
        HashMap<String, STC> globalSymbolTable = symbolTable.get(GLOBAL_SCOPE);
		
		if (scopedSymbolTable != null) {
            if (scopedSymbolTable.get(binaryOp.image) != null) {
                STC scopedVariable = scopedSymbolTable.get(binaryOp.image);
                scopedVariable.addData("readFrom", true);
                scopedSymbolTable.put(binaryOp.image, scopedVariable);
            }
        }

        if (globalSymbolTable != null) {
            if (globalSymbolTable.get(binaryOp.image) != null) {
                STC globalVariable = globalSymbolTable.get(binaryOp.image);
                globalVariable.addData("readFrom", true);
                globalSymbolTable.put(binaryOp.image, globalVariable);
            }
        }

        return node.jjtGetValue();

    }
	
	@Override
    public Object visit(ASTCompareOp node, Object data) {
		node.childrenAccept(this, data);
        return null;
    }
	
	@Override
    public Object visit(ASTstatement node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
	
}