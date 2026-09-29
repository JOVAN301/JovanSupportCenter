package com.jovan.supportcenter;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    LinearLayout content;
    final List<String> localRequests = new ArrayList<>();
    SharedPreferences prefs;
    ApiClient api;
    final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    int green = 0xFF0A8F55, dark = 0xFF063B2E;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("jsc", MODE_PRIVATE);
        api = new ApiClient(prefs.getString("api_base", "http://10.0.2.2:3000"));
        api.setToken(prefs.getString("token", null));
        seedRequests();
        showHome();
    }

    TextView title(String text, int size) { TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(dark); t.setGravity(Gravity.CENTER); t.setPadding(8,16,8,16); return t; }
    Button button(String text) { Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(16); return b; }
    EditText field(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setPadding(12,10,12,10); return e; }
    LinearLayout base(String heading) { ScrollView s=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(24,20,24,30); content.setBackgroundColor(0xFFF5FAF7); s.addView(content); setContentView(s); content.addView(title(heading,27)); return content; }
    void seedRequests(){ localRequests.add("Education • School fees for a student • UGX 800,000"); localRequests.add("Medical • Treatment support for a mother • UGX 700,000"); localRequests.add("Food • Family food support • UGX 500,000"); }
    boolean loggedIn(){ return api.getToken()!=null && !api.getToken().isEmpty(); }

    void showHome(){
        LinearLayout c=base("Jovan Support Center");
        TextView w=title("Real People • Real Help • A Better Tomorrow",16); w.setTextColor(green); c.addView(w);
        ImageView photo=new ImageView(this); photo.setImageResource(R.drawable.profile_photo); photo.setScaleType(ImageView.ScaleType.CENTER_CROP); c.addView(photo,new LinearLayout.LayoutParams(-1,260));
        c.addView(title("Together we can make a difference.",21));
        Button need=button("🙏 I Need Support"); need.setOnClickListener(v -> loggedIn()?showRequestForm():showAuthChoice("To create a support request, please create an account or log in first.")); c.addView(need);
        Button help=button("❤️ I Want to Help"); help.setOnClickListener(v->showRequests()); c.addView(help);
        Button activity=button("📋 Browse Support Requests"); activity.setOnClickListener(v->showRequests()); c.addView(activity);
        if(loggedIn()){ Button mine=button("📊 My Activity"); mine.setOnClickListener(v->showActivity()); c.addView(mine); }
        Button account=button(loggedIn()?"👤 My Account":"🔐 Create Account / Login"); account.setOnClickListener(v-> loggedIn()?showProfile():showAuthChoice("Create an account to submit and track support requests.")); c.addView(account);
        Button settings=button("⚙️ Server Settings"); settings.setOnClickListener(v->showServerSettings()); c.addView(settings);
    }

    void showAuthChoice(String msg){ new AlertDialog.Builder(this).setTitle("Jovan Support Center").setMessage(msg).setNegativeButton("Create Account",(d,w)->showRegister()).setPositiveButton("Log In",(d,w)->showLogin()).show(); }
    void showRegister(){
        LinearLayout c=base("Create Account"); EditText name=field("Full name"), email=field("Email"), phone=field("Phone number"), pass=field("Password (8+ characters)"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); c.addView(name);c.addView(email);c.addView(phone);c.addView(pass);
        Button b=button("Create Account"); b.setOnClickListener(v->{String n=name.getText().toString().trim(),e=email.getText().toString().trim(),p=phone.getText().toString().trim(),pw=pass.getText().toString(); if(n.isEmpty()||e.isEmpty()||pw.length()<8){toast("Enter your name, email and an 8+ character password.");return;} JSONObject j=new JSONObject(); try{j.put("name",n);j.put("email",e);j.put("phone",p);j.put("password",pw);}catch(Exception ignored){} b.setEnabled(false); api.register(j.toString(),authCallback(b,"Account created. Your verification status is pending."));}); c.addView(b); Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);
    }
    void showLogin(){
        LinearLayout c=base("Log In"); EditText email=field("Email"), pass=field("Password"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);Button b=button("Log In");b.setOnClickListener(v->{if(email.getText().toString().trim().isEmpty()||pass.getText().toString().isEmpty()){toast("Enter email and password.");return;}JSONObject j=new JSONObject();try{j.put("email",email.getText().toString().trim());j.put("password",pass.getText().toString());}catch(Exception ignored){}b.setEnabled(false);api.login(j.toString(),authCallback(b,"Login successful."));});c.addView(b);Button reg=button("Create a new account");reg.setOnClickListener(v->showRegister());c.addView(reg);Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);
    }
    Callback authCallback(Button source,String success){return new Callback(){public void onFailure(Call call,IOException e){ui.post(()->{source.setEnabled(true);toast("Could not reach the server. Check Server Settings.");});}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"{}";ui.post(()->{source.setEnabled(true);try{JSONObject j=new JSONObject(body);if(!r.isSuccessful()){toast(j.optString("error","Request failed"));return;}api.setToken(j.getString("token"));prefs.edit().putString("token",j.getString("token")).apply();new AlertDialog.Builder(MainActivity.this).setTitle("Success").setMessage(success).setPositiveButton("Continue",(d,w)->showHome()).show();}catch(Exception e){toast("Unexpected server response.");}});}};}

    void showRequestForm(){
        LinearLayout c=base("Create a Support Request"); TextView info=new TextView(this);info.setText("Your request will be submitted for verification before it appears publicly.");info.setTextSize(15);c.addView(info);
        EditText titleField=field("Short title");c.addView(titleField); Spinner category=new Spinner(this);String[] cats={"Education","Medical","Food","Rent","Emergency","Legal","Jobs","Other"};category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));c.addView(category);
        EditText description=field("Explain what support you need");description.setMinLines(4);description.setGravity(Gravity.TOP);c.addView(description);EditText amount=field("Amount needed (UGX)");amount.setInputType(InputType.TYPE_CLASS_NUMBER);c.addView(amount);EditText location=field("Location (optional)");c.addView(location);
        Button submit=button("Submit for Verification");submit.setOnClickListener(v->{if(titleField.getText().toString().trim().isEmpty()||description.getText().toString().trim().isEmpty()){toast("Enter a title and explain your need.");return;}JSONObject j=new JSONObject();try{j.put("title",titleField.getText().toString().trim());j.put("category",category.getSelectedItem().toString());j.put("description",description.getText().toString().trim());if(!amount.getText().toString().trim().isEmpty())j.put("amount_needed",Integer.parseInt(amount.getText().toString().trim()));j.put("location",location.getText().toString().trim());}catch(Exception e){toast("Please enter a valid amount.");return;}submit.setEnabled(false);api.createRequest(j.toString(),new Callback(){public void onFailure(Call call,IOException e){ui.post(()->{submit.setEnabled(true);toast("Could not submit. Check your connection.");});}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"{}";ui.post(()->{submit.setEnabled(true);try{JSONObject x=new JSONObject(body);if(!r.isSuccessful()){toast(x.optString("error","Submission failed"));return;}new AlertDialog.Builder(MainActivity.this).setTitle("Request Submitted").setMessage("Your request has been received and is pending verification.").setPositiveButton("OK",(d,w)->showHome()).show();}catch(Exception e){toast("Unexpected server response.");}});}});});c.addView(submit);Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);
    }

    void showRequests(){
        LinearLayout c=base("Support Requests");TextView info=new TextView(this);info.setText("Verified requests from the Jovan Support Center community.");info.setTextSize(15);c.addView(info);TextView loading=new TextView(this);loading.setText("Loading verified requests…");loading.setPadding(0,20,0,20);c.addView(loading);
        api.getPublicRequests(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->{loading.setText("The server is not reachable. Showing demo requests for now.");renderLocalRequests(c);});}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"[]";ui.post(()->{try{if(!r.isSuccessful()){loading.setText("Unable to load live requests.");renderLocalRequests(c);return;}JSONArray a=new JSONArray(body);c.removeView(loading);if(a.length()==0){TextView empty=new TextView(MainActivity.this);empty.setText("No verified requests yet. Check back soon.");empty.setTextSize(17);empty.setPadding(0,30,0,30);c.addView(empty);}for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);addRequestCard(c,x.optInt("id"),x.optString("category"),x.optString("title"),x.optString("description"),x.optInt("amount_needed"),x.optString("requester"));}}catch(Exception e){loading.setText("Unable to read server data.");renderLocalRequests(c);}});}});
        Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);
    }
    void renderLocalRequests(LinearLayout c){for(String r:localRequests){TextView card=new TextView(this);card.setText("Demo request\n\n"+r);card.setTextSize(16);card.setTextColor(dark);card.setPadding(18,18,18,18);card.setBackgroundColor(0xFFFFFFFF);c.addView(card);Button b=button("Support This Request");b.setOnClickListener(v->showDonation(0,r));c.addView(b);}}
    void addRequestCard(LinearLayout c,String cat,String title,String desc,int amount,String requester){
        addRequestCard(c,0,cat,title,desc,amount,requester);
    }
    void addRequestCard(LinearLayout c,int requestId,String cat,String title,String desc,int amount,String requester){
        TextView card=new TextView(this);String money=amount>0?"UGX "+String.format("%,d",amount):"Amount not specified";
        card.setText("✓ VERIFIED\n\n"+cat+" • "+title+"\n\n"+desc+"\n\nRequested by: "+requester+"\nAmount: "+money);card.setTextSize(16);card.setTextColor(dark);card.setPadding(18,18,18,18);card.setBackgroundColor(0xFFFFFFFF);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,14,0,0);c.addView(card,p);
        Button b=button("Support This Request");b.setOnClickListener(v->{ if(!loggedIn()) { showAuthChoice("Please create an account or log in before making a support contribution."); } else { showDonation(requestId,title); } });c.addView(b);
    }
    void showDonation(long requestId,String requestTitle){
        LinearLayout c=base("Support Someone");TextView t=new TextView(this);t.setText("Supporting: "+requestTitle);t.setTextSize(17);c.addView(t);
        EditText amount=field("Amount in UGX");amount.setInputType(InputType.TYPE_CLASS_NUMBER);c.addView(amount);
        Button donate=button("Record Support Contribution");donate.setOnClickListener(v->{
            if(requestId<=0){ new AlertDialog.Builder(this).setTitle("Demo Request").setMessage("This is a demo request. Live contributions are available when you select a verified request from the server.").setPositiveButton("OK",null).show(); return; }
            if(amount.getText().toString().trim().isEmpty()){toast("Enter an amount first.");return;}
            JSONObject j=new JSONObject();try{j.put("request_id",requestId);j.put("amount",Integer.parseInt(amount.getText().toString().trim()));j.put("provider","manual");}catch(Exception e){toast("Please enter a valid amount.");return;}
            donate.setEnabled(false);api.createContribution(j.toString(),new Callback(){public void onFailure(Call call,IOException e){ui.post(()->{donate.setEnabled(true);toast("Could not reach the server.");});}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"{}";ui.post(()->{donate.setEnabled(true);try{JSONObject x=new JSONObject(body);if(!r.isSuccessful()){toast(x.optString("error","Contribution failed"));return;}new AlertDialog.Builder(MainActivity.this).setTitle("Contribution Recorded").setMessage("Your support contribution has been recorded as pending. No money has been transferred yet.").setPositiveButton("OK",(d,w)->showHome()).show();}catch(Exception e){toast("Unexpected server response.");}});}});
        });c.addView(donate);Button back=button("← Back");back.setOnClickListener(v->showRequests());c.addView(back);
    }

    void showActivity(){
        LinearLayout c=base("My Activity");TextView loading=title("Loading your activity…",17);c.addView(loading);
        TextView section1=title("My Support Requests",21);c.addView(section1);
        api.getMine(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->loading.setText("Could not load activity."));}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"[]";ui.post(()->{try{JSONArray a=new JSONArray(body);if(!r.isSuccessful()){loading.setText("Could not load requests.");return;}loading.setText("");if(a.length()==0){TextView e=new TextView(MainActivity.this);e.setText("You have not submitted any support requests yet.");e.setPadding(0,10,0,20);c.addView(e);}for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);TextView row=new TextView(MainActivity.this);row.setText(x.optString("title")+"\nStatus: "+x.optString("status")+"\nCategory: "+x.optString("category")+"\nAmount: "+(x.optInt("amount_needed")>0?"UGX "+String.format("%,d",x.optInt("amount_needed")):"Not specified"));row.setTextSize(16);row.setPadding(14,14,14,14);c.addView(row);}}catch(Exception e){loading.setText("Unable to read request history.");}});}});
        TextView section2=title("My Contributions",21);c.addView(section2);TextView contrib=title("Loading contributions…",17);c.addView(contrib);
        api.getContributions(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->contrib.setText("Could not load contributions."));}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"[]";ui.post(()->{try{JSONArray a=new JSONArray(body);if(!r.isSuccessful()){contrib.setText("Could not load contributions.");return;}contrib.setText("");if(a.length()==0){contrib.setText("No contributions recorded yet.");}for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);TextView row=new TextView(MainActivity.this);row.setText(x.optString("title")+"\nUGX "+String.format("%,d",x.optInt("amount"))+" • "+x.optString("payment_status"));row.setPadding(14,14,14,14);row.setTextSize(16);c.addView(row);}}catch(Exception e){contrib.setText("Unable to read contributions.");}});}});
        TextView section3=title("Notifications",21);c.addView(section3);TextView notes=title("Loading notifications…",17);c.addView(notes);
        api.getNotifications(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->notes.setText("Could not load notifications."));}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"[]";ui.post(()->{try{JSONArray a=new JSONArray(body);if(!r.isSuccessful()){notes.setText("Could not load notifications.");return;}notes.setText("");if(a.length()==0){notes.setText("No notifications yet.");}for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);TextView row=new TextView(MainActivity.this);row.setText((x.optString("read_at").isEmpty()?"• ":"")+x.optString("message")+"\n"+x.optString("created_at"));row.setPadding(14,14,14,14);row.setTextSize(16);c.addView(row);}}catch(Exception e){notes.setText("Unable to read notifications.");}});}});
        Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);
    }

    void showProfile(){LinearLayout c=base("My Account");ImageView photo=new ImageView(this);photo.setImageResource(R.drawable.profile_photo);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);c.addView(photo,new LinearLayout.LayoutParams(-1,280));TextView details=new TextView(this);details.setText("Loading account…");details.setTextSize(17);details.setTextColor(dark);details.setPadding(8,20,8,20);c.addView(details);api.me(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->details.setText("Could not load your account details."));}public void onResponse(Call call,Response r)throws IOException{String body=r.body()!=null?r.body().string():"{}";ui.post(()->{try{JSONObject x=new JSONObject(body);details.setText("Name: "+x.optString("name")+"\nEmail: "+x.optString("email")+"\nPhone: "+x.optString("phone","Not provided")+"\n\nVerification: "+x.optString("verification_status")+"\n\nSupport Contact\n0758201628\njovialpro657@gmail.com\n\nSupport Bank Account\n1012103598274");}catch(Exception e){details.setText("Unable to read account details.");}});}});Button logout=button("Log Out");logout.setOnClickListener(v->{prefs.edit().remove("token").apply();api.setToken(null);showHome();});c.addView(logout);Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);}

    void showServerSettings(){LinearLayout c=base("Server Settings");TextView info=new TextView(this);info.setText("For testing: Android Emulator usually uses http://10.0.2.2:3000. For a physical phone, enter the computer's LAN address, for example http://192.168.1.20:3000.");info.setTextSize(15);c.addView(info);EditText url=field("API base URL");url.setText(prefs.getString("api_base","http://10.0.2.2:3000"));c.addView(url);Button save=button("Save Server Address");save.setOnClickListener(v->{String u=url.getText().toString().trim();if(u.isEmpty()){toast("Enter a server address.");return;}prefs.edit().putString("api_base",u).apply();api=new ApiClient(u);api.setToken(prefs.getString("token",null));toast("Server address saved.");showHome();});c.addView(save);Button test=button("Test Server");test.setOnClickListener(v->api.getPublicRequests(new Callback(){public void onFailure(Call call,IOException e){ui.post(()->toast("Server not reachable."));}public void onResponse(Call call,Response r){ui.post(()->toast(r.isSuccessful()?"Server is reachable.":"Server returned an error."));}}));c.addView(test);Button back=button("← Home");back.setOnClickListener(v->showHome());c.addView(back);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
