package com.cwc.litenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_edit extends Activity {

    private Long mRowId, mCreatedTime;
    private String mTitle, mPicFileName, mCameraPicFileName, mBody;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    Note_common note_common;
    private boolean mEnSaveDb = true;
    boolean bUseCameraPicture;
    DB mDb;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note_title);// set title
    	
        System.out.println("Note_edit / onCreate");
        
		if(Build.VERSION.SDK_INT >= 11)
		{
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
        
    	Bundle extras = getIntent().getExtras();
    	mRowId = extras.getLong(DB.KEY_NOTE_ID);
    	mPicFileName = extras.getString(DB.KEY_NOTE_PICTURE);
    	mTitle = extras.getString(DB.KEY_NOTE_TITLE);
    	mBody = extras.getString(DB.KEY_NOTE_BODY);
    	mCreatedTime = extras.getLong(DB.KEY_NOTE_CREATED);
        
    	//initialization
        note_common = new Note_common(this, mRowId, mTitle, mPicFileName, mBody, mCreatedTime);
        note_common.UI_init();
        mCameraPicFileName = "";
        bUseCameraPicture = false;
        
        // get picture file name in DB if instance is not null
        mDb = new DB(Note_edit.this);
        if(savedInstanceState != null)
        {
	        mDb.doOpen();
	        System.out.println("Note_edit / onCreate / mRowId =  " + mRowId);
	        if(mRowId != null)
	        {
	        	mPicFileName = mDb.getNotePictureStringById(mRowId);
	       		Note_common.mCurrentPictureFileName = mPicFileName;
	        }
	        mDb.doClose();
        }
        
    	// show view
        note_common.populateFields(mRowId);
		
		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
				if(note_common.bRemovePicFileName)
				{
					mPicFileName = "";
				}
                mEnSaveDb = true;
                finish();
            }

        });
        
        // delete button: delete note
        Button delButton = (Button) findViewById(R.id.note_edit_delete);
        delButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        // delete
        delButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
				//warning :start
        		mPref_delete_warn = getSharedPreferences("delete_warn", 0);
            	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
            	   mPref_delete_warn.getString("KEY_DELETE_NOTE_WARN","yes").equalsIgnoreCase("yes")) 
            	{
        			Util util = new Util(Note_edit.this);
    				util.vibrate();
            		
            		Builder builder1 = new Builder(Note_edit.this ); 
            		builder1.setTitle(R.string.confirm_dialog_title)
                        .setMessage(R.string.confirm_dialog_message)
                        .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{/*nothing to do*/}
                        })
                        .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{
                        		note_common.deleteNote(mRowId);
                            	finish();
                        	}
                        })
                        .show();//warning:end
            	}
            	else{
            	    //no warning:start
	                setResult(RESULT_CANCELED);
	                note_common.deleteNote(mRowId);
	                finish();
            	}
            }
        });
        
        // cancel button: leave, do not save current modification
        Button cancelButton = (Button) findViewById(R.id.note_edit_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                
                // check if note content is modified
               	if(note_common.isModified())
            	{
               		// show confirmation dialog
            		confirmToUpdateDlg();
            	}
            	else
            	{
            		mEnSaveDb = false;
                    finish();
            	}
            }
        });
    }
    
    // confirm to update change or not
    void confirmToUpdateDlg()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(Note_edit.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.edit_note_confirm_update)
	           // Yes, to update
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if(note_common.bRemovePicFileName)
						{
							mPicFileName = "";
						}
					    mEnSaveDb = true;
					    finish();
					}})
			   // cancel
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})
			   // no, roll back to original status		
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Bundle extras = getIntent().getExtras();
						String originalPictureFileName = extras.getString(DB.KEY_NOTE_PICTURE);

						if(originalPictureFileName.isEmpty())
						{   // no picture at first
							note_common.removePictureStringFromOriginalNote(mRowId);
		                    mEnSaveDb = false;
						}
						else
						{	// roll back existing picture
							Note_common.bRollBackData = true;
							mPicFileName = originalPictureFileName;
							mEnSaveDb = true;
						}	
	                    finish();
					}})
			   .show();
    }
    

    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
        
        System.out.println("Note_edit / onPause / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onPause / mPicFileName = " + mPicFileName);
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPicFileName); 
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        System.out.println("Note_edit / onSaveInstanceState / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onSaveInstanceState / bUseCameraPicture = " + bUseCameraPicture);
        System.out.println("Note_edit / onSaveInstanceState / mCameraPicFileName = " + mCameraPicFileName);
        
        
        if(note_common.bRemovePicFileName)
    	    outState.putBoolean("removeOriginalPictureFileName",true);
        
        if(bUseCameraPicture)
        {
        	outState.putBoolean("UseCameraPicture",true);
        	outState.putString("showCameraPictureFileName", mPicFileName);
        }
        else
        {
        	outState.putBoolean("UseCameraPicture",false);
        	outState.putString("showCameraPictureFileName", "");
        }
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPicFileName); 
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
        
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseCameraPicture"))
    		bUseCameraPicture = true;
    	else
    		bUseCameraPicture = false;
    	
    	mCameraPicFileName = savedInstanceState.getString("showCameraPictureFileName");
    	
    	System.out.println("Note_edit / onRestoreInstanceState / savedInstanceState.getBoolean removeOriginalPictureFileName =" +
    							savedInstanceState.getBoolean("removeOriginalPictureFileName"));
        if(savedInstanceState.getBoolean("removeOriginalPictureFileName"))
        {
        	mCameraPicFileName = "";
        	note_common.mOriginalPictureFileName="";
        	Note_common.mCurrentPictureFileName="";
        	note_common.removePictureStringFromOriginalNote(mRowId);
        	note_common.populateFields(mRowId);
        	note_common.bRemovePicFileName = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
	    if(UtilImage.bShowExpandedImage == true)
	    {
	    	UtilImage.closeExpandImage();
	    }
	    else
	    {
	    	if(note_common.isModified())
	    	{
	    		confirmToUpdateDlg();
	    	}
	    	else
	    	{
	            mEnSaveDb = false;
	            finish();
	    	}
	    }
    }
    
    static final int TAKE_PICTURE = R.id.TAKE_PICTURE;
	private static int TAKE_PICTURE_ACT = 1;    
	private Uri imageUri;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(Build.VERSION.SDK_INT >= 11)
		{
		    menu.add(0, TAKE_PICTURE, 0, R.string.note_take_picture )
		    .setIcon(android.R.drawable.ic_menu_camera)
		    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}
		else
		{	
			menu.add(0, TAKE_PICTURE, 0,  R.string.note_take_picture )
		    .setIcon(R.drawable.ic_input_add);
		}	

		return super.onCreateOptionsMenu(menu);
	}
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
        {
		    case android.R.id.home:
		    	NavUtils.navigateUpTo(this, new Intent(this, TabsHost.class));
		        return true;
	        
            case TAKE_PICTURE:
            	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            	// new picture file name with current time stamp
				mPicFileName = Util.getCurrentTimeString() + ".jpg";
            	imageUri = UtilImage.getPictureUri(mPicFileName, Note_edit.this);
			    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			    startActivityForResult(intent, TAKE_PICTURE_ACT); 
			    mEnSaveDb = true;
			    note_common.bRemovePicFileName = false; // reset
			    
			    if(UtilImage.mExpandedImageView != null)
			    	UtilImage.closeExpandImage();
		        
			    return true;
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		if (requestCode == TAKE_PICTURE_ACT)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				System.out.println("Note_edit / onActivity / .RESULT_OK");
				imageUri = UtilImage.getPictureUri(Note_common.mCurrentPictureFileName, Note_edit.this);
				String str = getResources().getText(R.string.note_take_picture_OK ).toString();
	            Toast.makeText(Note_edit.this, str + " " + imageUri.toString(), Toast.LENGTH_SHORT).show();
	            note_common.populateFields(mRowId);
	            bUseCameraPicture = true;
	            mCameraPicFileName = Note_common.mCurrentPictureFileName;
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				System.out.println("Note_edit / onActivity / .RESULT_CANCELED");
				bUseCameraPicture = false;
				// to use captured picture or original picture
				if(!mCameraPicFileName.isEmpty())
				{
					// update
					Note_common.saveStateInDB(mRowId,mEnSaveDb,mCameraPicFileName); // replace with existing picture
					note_common.populateFields(mRowId);
		            
					// set for Rotate any times
		            bUseCameraPicture = true;
		            mPicFileName = Note_common.mCurrentPictureFileName; // for pause
		            mCameraPicFileName = Note_common.mCurrentPictureFileName; // for save instance

				}
				else
				{
					// skip new file name, roll back to original one
			    	Note_common.mCurrentPictureFileName = note_common.mOriginalPictureFileName;
			    	mPicFileName = note_common.mOriginalPictureFileName;
					Toast.makeText(Note_edit.this, R.string.note_take_picture_Cancel, Toast.LENGTH_LONG).show();
				}
				
				mEnSaveDb = true;
				Note_common.saveStateInDB(mRowId,mEnSaveDb,mPicFileName);
				note_common.populateFields(mRowId);
			}
		}
		
        if(requestCode == Util.ACTIVITY_SELECT_PICTURE && resultCode == Activity.RESULT_OK)
        {
			Bundle extras = imageReturnedIntent.getExtras();
			String fileName = extras.getString("FILENAME"); 
        	System.out.println("check onActivityResult / fileName = " + fileName);
        	
        	mRowId = Note_common.saveStateInDB(mRowId,true,fileName); 
        	
            note_common.populateFields(mRowId);
			
            // set for Rotate any times
            bUseCameraPicture = true;
            mPicFileName = Note_common.mCurrentPictureFileName; // for pause
            mCameraPicFileName = Note_common.mCurrentPictureFileName; // for save instance

        }   		
	}
}
