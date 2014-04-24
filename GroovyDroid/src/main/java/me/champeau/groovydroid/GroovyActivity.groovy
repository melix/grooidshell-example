package me.champeau.groovydroid

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.EditText
import com.android.dx.dex.DexFormat
import com.android.dx.dex.DexOptions
import com.android.dx.dex.cf.CfOptions
import com.android.dx.dex.cf.CfTranslator
import com.android.dx.dex.code.PositionList
import com.android.dx.dex.file.DexFile
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer

@CompileStatic
class GroovyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.groovy_main)
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.groovy, menu)
        true
    }

    public void executeCode(View view) {
        def resultView = (EditText) findViewById(R.id.resultView)
        resultView.setText(generateMessage())
    }

    String generateMessage() {
        long sd = System.currentTimeMillis()
        def dexOptions = new DexOptions()
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES
        def cfOptions = new CfOptions()
        cfOptions.positionInfo = PositionList.LINES
        cfOptions.localInfo = true
        cfOptions.strictNameCheck = true
        cfOptions.optimize = false
        cfOptions.optimizeListFile = null
        cfOptions.dontOptimizeListFile = null
        cfOptions.statistics = false
        def config = new CompilerConfiguration()
        def bytecode
        def className
        config.bytecodePostprocessor = { String s, byte[] bytes ->
            def classDefItem = CfTranslator.translate("${s}.class", bytes, cfOptions, dexOptions)
            def df = new DexFile(dexOptions)
            df.add(classDefItem)
            byte[] result
            try {
                result = df.toDex(new OutputStreamWriter(new ByteArrayOutputStream()), false)
                className = s
            } catch (IOException e) {
                Log.e("DalvikConversion", "Unable to convert to Dalvik", e)
                result = bytes
            }
            bytecode = result
            result
        }
        def gcl = new GroovyClassLoader(this.classLoader, config)
        def code = (EditText) findViewById(R.id.editText)
        try {

            gcl.parseClass(code.getText().toString());
        }  catch (Throwable e) {
            Log.e("Dynamic","Dynamic loading failed but intercepted bytecode ($bytecode)")
        }
        GroovyDroidClassLoader droidClassLoader =
                new GroovyDroidClassLoader(applicationContext.getDir("dynclasses", 0), this.classLoader)
        Class<? extends Script> scriptClass = droidClassLoader.defineDynamic(className, bytecode)
        long compileTime = System.currentTimeMillis()-sd
        Script script
        def eval
        long execTime = 0
        try {
            sd = System.currentTimeMillis()
            script = scriptClass.newInstance()
            eval = script.run()
            execTime = System.currentTimeMillis()-sd
        } catch (Throwable e) {
            Log.e("Dynamic", "Unable to create script",e)
            eval = e.message
        }
        """Compile time:${compileTime}ms
Exec time: ${execTime}ms
Result: $eval"""
    }

}
