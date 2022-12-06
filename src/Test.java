import service.CommandLineParser;

public class Test {
	public static void main(String[] args) {
		CommandLineParser clp = new CommandLineParser(args);
		
		// inside main method
	    boolean client = clp.getFlag("client");
	    boolean demon = clp.getFlag("demon");
	    String config = clp.getArgumentValue("config")[0];
	    String log = clp.getArgumentValue("log")[0];
	    int head = Integer.parseInt(clp.getArgumentValue("head")[0]);
	    String time1 = clp.getArgumentValue("time")[0];
	    String time2 = clp.getArgumentValue("time")[1];
			String tag = clp.getArgumentValue("tag")[0];
			String name = clp.getArgumentValue("name")[0];

	    System.out.println("client : " + client);
	    System.out.println("demon : " + demon);
	    System.out.println("config : " + config);
	    System.out.println("log : " + log);
	    System.out.println("head : " + head);
	    System.out.println("time1 : " + time1);
	    System.out.println("time2 : " + time2);
	    if(tag != null) {
	    System.out.println("tag : " + tag);
	    	}
	    if(name != null) {
	    System.out.println("name : " + name);
	    	}
	}
}
