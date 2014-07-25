package me.champeau.groovydroid

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.EditText
import groovy.transform.CompileStatic
import me.champeau.testlibrary.groovylib.GroovyUtils

@CompileStatic
class GroovyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.groovy_main)
        Log.d("GroovyActivity", GroovyUtils.hello())
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
        GrooidShell shell = new GrooidShell(applicationContext.getDir("dynclasses", 0), this.classLoader)

        def code = (EditText) findViewById(R.id.editText)
        shell.evaluate(code.text.toString())
    }

}
