package me.champeau.groovydroid;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.android.dx.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;

import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicReference;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

public class GroovyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groovy_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.groovy, menu);
        return true;
    }

    public void executeCode(View view) {
        final EditText resultView = (EditText) findViewById(R.id.resultView);
        resultView.setText(generateMessage());
    }

    String generateMessage() {
        final DexOptions dexOptions = new DexOptions();
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
        final CfOptions cfOptions = new CfOptions();

        cfOptions.positionInfo = PositionList.LINES;
        cfOptions.localInfo = true;
        cfOptions.strictNameCheck = true;
        cfOptions.optimize = false;
        cfOptions.optimizeListFile = null;
        cfOptions.dontOptimizeListFile = null;
        cfOptions.statistics = false;

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(
                new ASTTransformationCustomizer(CompileStatic.class)
        );
        final AtomicReference<byte[]> bytecode = new AtomicReference<byte[]>();
        final AtomicReference<String> className = new AtomicReference<String>();
        config.setBytecodePostprocessor(new BytecodeProcessor() {
            @Override
            public byte[] processBytecode(String s, byte[] bytes) {
                ClassDefItem classDefItem = CfTranslator.translate(s + ".class", bytes, cfOptions, dexOptions);
                DexFile df = new DexFile(dexOptions);
                df.add(classDefItem);
                byte[] result;
                try {
                    result = df.toDex(new OutputStreamWriter(new ByteArrayOutputStream()), false);
                    className.set(s);
                } catch (IOException e) {
                    Log.e("DalvikConversion", "Unable to convert to Dalvik", e);
                    result = bytes;
                }
                bytecode.set(result);
                return result;
            }
        });
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClassLoader(),config);
        EditText code = (EditText) findViewById(R.id.editText);
        try {

            gcl.parseClass(code.getText().toString());
        }/* catch (MultipleCompilationErrorsException e) {
            return e.getMessage();
        } */catch (Throwable e) {
            Log.e("Dynamic","Dynamic loading failed but intercepted bytecode ("+bytecode.get()+")");
        }
        GroovyDroidClassLoader droidClassLoader =
                new GroovyDroidClassLoader(getApplicationContext().getDir("dynclasses", 0), this.getClassLoader());
        Class<? extends Script> scriptClass = droidClassLoader.defineDynamic(className.get(), bytecode.get());
        Script script = null;
        try {
            script = scriptClass.newInstance();
        } catch (InstantiationException e) {
            Log.e("Dynamic", "Unable to create script",e) ;
            return e.getMessage();
        } catch (IllegalAccessException e) {
            Log.e("Dynamic", "Unable to create script",e) ;
            return e.getMessage();
        } catch (Throwable e) {
            Log.e("Dynamic", "Unable to create script",e) ;
            return e.getMessage();
        }
        return DefaultGroovyMethods.asType(script.run(), String.class);
    }

}
