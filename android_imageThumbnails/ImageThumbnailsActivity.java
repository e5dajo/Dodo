package image.Thumbnails;


import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;


public class ImageThumbnailsActivity extends Activity
{
	public MediaStructure mediaStructure;
    public MediaStructure activeStructure;
    public ImageAdapter ia;
    public MediaPlayer mediaPlay;
    //public String basePath = "/mnt/sdcard/My SugarSync Folders/pics/";
    public String basePath = "/mnt/sdcard/Images/";
    final int doubleClickProtectionTime = 700;
    boolean doubleClickProtected = false;
    
    
    //used to protect from accidential double click
    Handler handler=new Handler();
    Runnable r=new Runnable()
    {
        public void run() 
        {
        	doubleClickProtected = false;                       
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Hide Title bar
        setContentView(R.layout.main);
        init_phone_image_grid();
    }

    
    //Catch click on Ancroid back button and do the same as when our back is clicked. (App will not terminate)
    @Override
    public void onBackPressed() {
    	if (activeStructure.parentStructure != null && !doubleClickProtected)
    	{
    		activeStructure=activeStructure.parentStructure;
    		this.ia.notifyDataSetChanged();
    		
    		 //Protect from fast double click
            doubleClickProtected = true;
            handler.postDelayed(r, doubleClickProtectionTime );
    		
    		
    		
    	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	doubleClickProtected = true;
    	handler.postDelayed(r, doubleClickProtectionTime );                          
    }
    
    
    
    private void init_phone_image_grid()
    {
        
    	//this.mediaStructure = new MediaStructure("/mnt/sdcard/My SugarSync Folders/pics/");
    	this.mediaStructure = new MediaStructure(basePath);
    	activeStructure = mediaStructure;
        this.activeStructure.media=this.mediaStructure.media;
        this.mediaPlay = new MediaPlayer();  
        GridView imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
        ImageView backView = (ImageView) findViewById(R.id.BackImageView);
        this.ia = new ImageAdapter(getApplicationContext());
        imagegrid.setAdapter(ia);
        backView.setClickable(true);
       
        
        backView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (activeStructure.parentStructure != null && !doubleClickProtected)
		    	{
		    		activeStructure=activeStructure.parentStructure;
		    		ia.notifyDataSetChanged();
		    		
		    		 //Protect from fast double click
		            doubleClickProtected = true;
		            handler.postDelayed(r, doubleClickProtectionTime );
		    	}
			}
        });
        //imageCursor.close
        
        
        imagegrid.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View v, int position, long id)
            {
            	
            	if (!doubleClickProtected){
            		
            		//if the image selected has a directory, set activeStructure to point at this directory structure
            		if (activeStructure.media[position].isDir)
            		{
            			if (activeStructure.media[position].isSound && !mediaPlay.isPlaying()) playSound(activeStructure.media[position].audioFilename);
            			int dirIndex=activeStructure.media[position].dirIndex;
            			activeStructure=activeStructure.subStructure[dirIndex];
            			ia.notifyDataSetChanged();
            			
            			 //Protect from fast double click
                        doubleClickProtected = true;
                        handler.postDelayed(r, doubleClickProtectionTime );
            		}
            		else 
            		{
            		//Show single image
            			if (activeStructure.media[position].isSound && !mediaPlay.isPlaying()) playSound(activeStructure.media[position].audioFilename);
            			final Intent intent = new Intent(ImageThumbnailsActivity.this, ViewImage.class);
            			intent.putExtra("filename", activeStructure.media[position].filename);
            			intent.putExtra("caption", activeStructure.media[position].getCaption());
            			intent.putExtra("audioFileName",activeStructure.media[position].audioFilename);
                    
                    
            			startActivityForResult(intent,0);
            		}
            	}
            }
        });
        //imageCursor.close();
    }

    
    public void playSound(String audioFile)
    {
    	try {
    		mediaPlay.reset();
			mediaPlay.setDataSource(audioFile);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    	try {
			mediaPlay.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	mediaPlay.start(); 
    }
    
    
    //MediaData holds all information for a mediaItem such as paths to bitmap and sound file
    public class MediaData
    {
    	public String filename;
    	public Bitmap image;
    	public boolean isDir;
    	public boolean isSound;
    	public int dirIndex;
    	public String audioFilename;    	
    	public MediaData (String fname,Bitmap img, boolean isDirectory, int index, String audioFname)
    	{
    		
    		dirIndex=index;
    		image=img;
    		filename=fname;
    		isDir=isDirectory;
    		isSound = (audioFname.length()>0);
    		audioFilename=audioFname;    			
    	}
    	
    	//GetCaption is used to get a string to write above each image. (Upper case filename without extension)
    	public String getCaption(){
    		int start= filename.lastIndexOf("/");
    		int stop= filename.lastIndexOf(".");
    		if (stop < 0) stop=filename.length(); //should not happen?
    		return filename.substring(start+1, stop).toUpperCase();
    	}
    }
    
    
    //MediaStructure holds a tree with all media found below basePath. It there is a directory with the same name as a image file a subStructure is created
    public class MediaStructure 
    {
    	public MediaStructure[] subStructure;
    	private MediaData[] media;    	
    	private int nrOfImages;
    	private int nrOfDirs;
    	public MediaStructure parentStructure;
    	private String path;
    	private int[] idArray;
    	public MediaStructure(String dirPath)
    	{
    		final String[] columns = { MediaStore.Images.Media._ID , MediaStore.Images.Media.DATA };
            final String orderBy = MediaStore.Images.Media.DATA;
            this.path=dirPath;
            String tempPath= "%" + path + "%"; 	
            String [] queryPath = {tempPath};
          //Query for images below dirPath. ANNOING that it is not possible to get just this folder without subfolders!!!! MAkes code messy...             
            Cursor imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,MediaStore.Images.Media.DATA + " like ? ", queryPath,orderBy);
            
            final String[] columnsAudio = { MediaStore.Audio.Media._ID , MediaStore.Audio.Media.DATA };
            //Query for Audio files  
            Cursor audioCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columnsAudio, MediaStore.Images.Media.DATA + " like ? ",queryPath, MediaStore.Audio.Media._ID);
            audioCursor.moveToFirst();
            int dataColIndexAudio = -1;
            if (audioCursor.getCount() > 0)
            {
            	dataColIndexAudio = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            }
            
            
            int dataColIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int image_column_index = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
            
            //getNrOfImages is called to find out how many images that are in the specific folder, ie not in subfolders.
            this.nrOfImages = getNrOfImages(imageCursor, this.path, dataColIndex);
            idArray=new int[this.nrOfImages];
            
            // Create array idArray with index for images located in path folder (i.e not in sub folders).
            int k=0;            
            for(int i=0;i<imageCursor.getCount();i++) 
            {
            	imageCursor.moveToPosition(i);
        		String s2 = stripFileExtension(imageCursor.getString(dataColIndex));
        		if (path.lastIndexOf("/") == s2.lastIndexOf("/"))
        		{
        					idArray[k]=i;
                          	k++;
        		}
            }
            
            // Build an array of MediaData getting data from cursor at indexes in idArray 
            media = new MediaData [this.nrOfImages];
            nrOfDirs = 0;
            for(int j=0;j<(this.nrOfImages);j++)        		
            {
                imageCursor.moveToPosition(idArray[j]);
                Bitmap img = MediaStore.Images.Thumbnails.getThumbnail(getApplicationContext().getContentResolver(),imageCursor.getInt(image_column_index), MediaStore.Images.Thumbnails.MINI_KIND, null);
                //Bitmap img = MediaStore.Images.Thumbnails.getThumbnail(getApplicationContext().getContentResolver(),imageCursor.getInt(image_column_index), MediaStore.Images.Thumbnails.MICRO_KIND, null);                
                String filename= imageCursor.getString(dataColIndex);
                File dir = new File (stripFileExtension(filename));
                boolean isDir = (dir.isDirectory());
                if (isDir) 
                {
                	nrOfDirs++;                	
                }
                if (audioCursor.getCount() > 0)
                {
                	//getAudioFilename searches for audio file within audioCursor and returns path to audio file if found.
                	media[j]=new MediaData(filename,img,isDir,nrOfDirs-1,getAudioFilename(audioCursor,filename,dataColIndexAudio));
                }
                else
                {
                	media[j]=new MediaData(filename,img,isDir,nrOfDirs-1,"");
                }
                	
            }
            
            // If any of the images has a dir, do recursive call to create mediastructure for this
            subStructure = new MediaStructure[nrOfDirs];
    		int i = 0;
            for(int l=0;l<(this.nrOfImages);l++)
            {
            	if (media[l].isDir)
            	{
            		String path_= stripFileExtension(media[l].filename);
            		path_ = path_ + "/";
            		subStructure[i]=new MediaStructure(path_);
            		subStructure[i].parentStructure=this;
            		i++;
            	}
            }
    		            	
    	}    	
    }
    
    
    public static String stripFileExtension(String fileName)
    {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0) ? fileName.substring(0, dotInd) : fileName;
    }
    
    
    
    //Find out how many images that are in the path directory, ie not in sub directories.
    public int getNrOfImages(Cursor imageCursor, String path, int dataColIndex)
    {
    	int picsInDir = 0;
    	for(int j=0;j<(imageCursor.getCount());j++)
            
    	{
    	
    		imageCursor.moveToPosition(j);
    		if (path.lastIndexOf("/") == stripFileExtension(imageCursor.getString(dataColIndex)).lastIndexOf("/"))
       		{
        		picsInDir++;
        	}
            
        }
    	return picsInDir;
    }
    
    
    //getAudioFilename searches for audio file within audioCursor and returns path to audio file if found.
    public String getAudioFilename(Cursor audioCursor, String filename, int dataColIndexAudio)
    {
    	if (audioCursor.getCount()==0) return "";
    	for(int j=0;j<(audioCursor.getCount());j++)
    	{
    		audioCursor.moveToPosition(j);
			if (stripFileExtension(filename).equalsIgnoreCase(stripFileExtension(audioCursor.getString(dataColIndexAudio))))
			{
				return audioCursor.getString(dataColIndexAudio);
			}
    	}
    	return "";    		
    }
    
    
    
    
    
    public class ImageAdapter extends BaseAdapter
    {
        private Context mContext;

        public ImageAdapter(Context c)
        {
            mContext = c;
        }

        @Override
        public int getCount()
        {
            return activeStructure.media.length;
        }

        @Override
        public Object getItem(int position)
        {
            return position;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }
        

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if(v!=null)
            {
            	LayoutInflater li = getLayoutInflater(); // use inflator to get both text and image in the gridview
            	v = li.inflate(R.layout.gridlayout, null);
            	TextView tv = (TextView)v.findViewById(R.id.gridText);
            	tv.setText(activeStructure.media[position].getCaption());
            	ImageView iv = (ImageView)v.findViewById(R.id.gridImage);
            	iv.setImageBitmap(activeStructure.media[position].image);
            }
            else
            {
            	v = new View(mContext.getApplicationContext());
            	LayoutInflater li = getLayoutInflater();
            	v = li.inflate(R.layout.gridlayout, null);
            	TextView tv = (TextView)v.findViewById(R.id.gridText);
            	tv.setText(activeStructure.media[position].getCaption());
            	ImageView iv = (ImageView)v.findViewById(R.id.gridImage);
            	iv.setImageBitmap(activeStructure.media[position].image);
            }
            return v;
        }
    }
}