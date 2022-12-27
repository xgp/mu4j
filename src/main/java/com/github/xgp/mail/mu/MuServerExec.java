package com.github.xgp.mail.mu;

import de.tudresden.inf.lat.jsexp.Sexp;
import de.tudresden.inf.lat.jsexp.SexpFactory;
import de.tudresden.inf.lat.jsexp.SexpList;
import de.tudresden.inf.lat.jsexp.SexpParserException;
import de.tudresden.inf.lat.jsexp.SexpString;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;

public class MuServerExec {

    private ExecutorService exec;

    public static void main(String[] argv) throws Exception {
	PipedOutputStream output = new PipedOutputStream();
	PipedInputStream input = new PipedInputStream(output);

	StartedProcess process = new ProcessExecutor().command("mu", "server")
	    .redirectInput(input)
	    .redirectOutput(new MuOutputStream() {
		    @Override
		    protected void processLine(String line) {
			System.out.println("line: "+line);
		    }
		    @Override
		    protected void processSexpr(String sexpr) {
			//			System.out.println("sexpr: "+sexpr);
			try {
			    Sexp parsed = SexpFactory.parse(new StringReader(sexpr.trim()));
			    Map<String, String> map = new HashMap<String, String>();
			    Iterator<Sexp> it = parsed.iterator();
			    while (it.hasNext()) {
				Sexp list = it.next();
				System.out.println(String.format("depth: %d, length: %d, atomic: %b\n%s",
								 list.getDepth(), list.getLength(),
								 list.isAtomic(), list.toIndentedString()));
			    }
			} catch (IOException | SexpParserException e) {
			    e.printStackTrace();
			}
		    }
		})
	    .start();

	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
	Thread.sleep(2000);

        writer.write("(ping)");
        writer.flush();
        writer.close();
        /*
        writer.write("(ping)\n");
        writer.flush();

        writer.write("(find query:links)\n");
        writer.flush();

        writer.close();
        */
        Thread.sleep(5000);
	process.getFuture().get(5, TimeUnit.SECONDS);
    }


    /*
       find    Using the find command we can search for messages.
              -> cmd:find query:"<query>" [threads:true|false] [sortfield:<sortfield>]
                 [reverse:true|false] [maxnum:<maxnum>]
              The  query-parameter  provides  the  search  query;  the threads-parameter determines whether the results will be returned in threaded fashion or not; the sortfield-parameter (a string, "to",
              "from", "subject", "date", "size", "prio") sets the search field, the reverse-parameter, if true, set the sorting order Z->A and, finally, the maxnum-parameter limits the number of results to
              return (<= 0 means 'unlimited').

cmd:find query:links
     */
}
