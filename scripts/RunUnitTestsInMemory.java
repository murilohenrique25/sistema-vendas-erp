import java.io.*;
import java.net.URI;
import java.util.*;
import javax.tools.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Explicit unit-test sources only. Does not start the application or write compiled classes/reports. */
class RunUnitTestsInMemory {
 public static void main(String[] args)throws Exception {
  if(args.length<2)throw new IllegalArgumentException("Usage: java --class-path <dependencies;target/classes> scripts/RunUnitTestsInMemory.java <test-class> <source-files...>");
  var compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("Requires a JDK.");
  var bytes=new HashMap<String,ByteArrayOutputStream>();
  try(var standard=compiler.getStandardFileManager(null,null,null)){
   var manager=new ForwardingJavaFileManager<StandardJavaFileManager>(standard){
    @Override public JavaFileObject getJavaFileForOutput(Location location,String name,JavaFileObject.Kind kind,FileObject sibling){
     return new SimpleJavaFileObject(URI.create("memory:///"+name.replace('.','/')+kind.extension),kind){
      @Override public OutputStream openOutputStream(){var stream=new ByteArrayOutputStream();bytes.put(name,stream);return stream;}
     };
    }
   };
   var sources=standard.getJavaFileObjectsFromStrings(Arrays.asList(args).subList(1,args.length));
   boolean success=compiler.getTask(null,manager,null,List.of("--release","21","-proc:none","-classpath",System.getProperty("java.class.path")),null,sources).call();
   if(!success)throw new IllegalStateException("In-memory compilation failed.");
  }
  var loader=new ClassLoader(RunUnitTestsInMemory.class.getClassLoader()){
   @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    Class<?> type=findLoadedClass(name);if(type==null){var output=bytes.get(name);if(output==null)return super.loadClass(name,resolve);byte[] data=output.toByteArray();type=defineClass(name,data,0,data.length);}if(resolve)resolveClass(type);return type;
   }
  };
  Thread.currentThread().setContextClassLoader(loader);
  var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(loader.loadClass(args[0]))).build();
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request);
  var summary=listener.getSummary();var writer=new PrintWriter(System.out,true);summary.printTo(writer);summary.printFailuresTo(writer);
  if(summary.getTestsFoundCount()==0||summary.getTestsFailedCount()!=0||summary.getTotalFailureCount()!=0)throw new IllegalStateException("Unit tests failed or no tests found.");
 }
}
