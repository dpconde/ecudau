/*
 * (C) Copyright 2015 by fr3ts0n <erwin.scheuch-heilig@gmx.at>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package com.fr3ts0n.ecu.gui.androbd.view;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Toast;

import com.fr3ts0n.ecu.EcuDataItem;
import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.gui.androbd.services.bluetooth.BtCommService;
import com.fr3ts0n.ecu.gui.androbd.services.bluetooth.BtDeviceListActivity;
import com.fr3ts0n.ecu.gui.androbd.services.bluetooth.CommService;
import com.fr3ts0n.ecu.gui.androbd.model.bo.FuelBO;
import com.fr3ts0n.ecu.gui.androbd.obdadapters.DfcItemAdapter;
import com.fr3ts0n.ecu.gui.androbd.obdadapters.ObdItemAdapter;
import com.fr3ts0n.ecu.gui.androbd.obdadapters.VidItemAdapter;
import com.fr3ts0n.ecu.gui.androbd.model.ObdData;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.gui.androbd.services.RouteService;
import com.fr3ts0n.ecu.prot.obd.ElmProt;
import com.fr3ts0n.ecu.prot.obd.ObdProt;
import com.fr3ts0n.pvs.PvChangeEvent;
import com.fr3ts0n.pvs.PvChangeListener;
import com.fr3ts0n.pvs.PvList;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Main Activity for Ecudau app
 */
public class MainActivity extends AppCompatActivity
	implements
		PvChangeListener,
		NewRouteFragment.OnFragmentInteractionListener,
		PropertyChangeListener,
		SharedPreferences.OnSharedPreferenceChangeListener,
		AbsListView.MultiChoiceModeListener
{
	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}

	@Override
	public void onFragmentInteraction(Uri uri) {

	}

	/**
	 * operating modes
	 */
	public enum MODE
	{
		OFFLINE,//< OFFLINE mode
		ONLINE,	//< ONLINE mode
		DEMO,	//< DEMO mode
		FILE,   //< FILE mode
	}

	/**
	 * data view modes
	 */
	public enum DATA_VIEW_MODE
	{
		LIST,       //< data list (un-filtered)
		FILTERED,   //< data list (filtered)
		DASHBOARD,  //< dashboard
		HEADUP,     //< Head up display
		CHART,		//< Chart display
	}

	/**
	 * Preselection types
	 */
	public enum PRESELECT
	{
		LAST_DEV_ADDRESS,
		LAST_ECU_ADDRESS,
		LAST_SERVICE,
		LAST_ITEMS,
		LAST_VIEW_MODE,
	}
	
	/**
	 * Key names for preferences
	 */
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	public static final String MEASURE_SYSTEM = "measure_system";
	public static final String ELM_ADAPTIVE_TIMING = SettingsActivity.ELM_TIMING_SELECT;
	public static final String ELM_RESET_ON_NRC = "elm_reset_on_nrc";
	public static final String PREF_USE_LAST = "USE_LAST_SETTINGS";
	public static final String PREF_AUTOHIDE = "autohide_toolbar";
	public static final String PREF_OVERLAY = "toolbar_overlay";
	public static final String PREF_DATA_DISABLE_MAX = "data_disable_max";

	/**
	 * Message types sent from the BluetoothChatService Handler
	 */
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_FILE_WRITTEN = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_DATA_ITEMS_CHANGED = 6;
	public static final int MESSAGE_UPDATE_VIEW = 7;
	public static final int MESSAGE_OBD_STATE_CHANGED = 8;
	public static final int MESSAGE_OBD_NUMCODES = 9;
	public static final int MESSAGE_OBD_ECUS = 10;
	public static final int MESSAGE_OBD_NRC = 11;
	public static final int MESSAGE_TOOLBAR_VISIBLE = 12;
	private static final String TAG = "Ecudau";

	/**
	 * internal Intent request codes
	 */
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int REQUEST_SETTINGS = 5;
	private static final int REQUEST_GRAPH_DISPLAY_DONE = 7;

	/**
	 * app exit parameters
	 */
	private static final int EXIT_TIMEOUT = 200;

	/**
	 * time between display updates to represent data changes
	 */
	private static final int DISPLAY_UPDATE_TIME = 550;
	public static final String KEEP_SCREEN_ON = "keep_screen_on";
	public static final String ELM_CUSTOM_INIT_CMDS = "elm_custom_init_cmds";

	private static final Logger log = Logger.getLogger(TAG);

	/** dialog builder */
	private static AlertDialog.Builder dlgBuilder;

	/**
	 * app preferences ...
	 */
	public static SharedPreferences prefs;
	/**
	 * Member object for the BT comm services
	 */
	private static CommService mCommService = null;
	/**
	 * Local Bluetooth adapter
	 */
	private static BluetoothAdapter mBluetoothAdapter = null;
	/**
	 * Name of the connected BT device
	 */
	private static String mConnectedDeviceName = null;

	/**
	 * Data list adapter
	 */
	private static ObdItemAdapter mPidAdapter;
	private static VidItemAdapter mVidAdapter;
	private static DfcItemAdapter mDfcAdapter;
	private static ObdItemAdapter currDataAdapter;
	/**
	 * Timer for display updates
	 */
	private static Timer updateTimer = new Timer();
	/**
	 * initial state of bluetooth adapter
	 */
	private static boolean initialBtStateEnabled = false;
	/**
	 * last time of back key pressed
	 */
	private static long lastBackPressTime = 0;
	/**
	 * toast for showing exit message
	 */
	private static Toast exitToast = null;

	/**
	 * current operating mode
	 */
	private MODE mode = MODE.OFFLINE;

	/** empty string set as default parameter*/
	static final Set<String> emptyStringSet = new HashSet<String>();


	/** Realm instance **/
	Realm realm;

	/** View instances **/
	private Toolbar toolbar;
	private Drawer drawerMenu;
	private RouteService routeService;
	public FragmentManager fragmentManager = getFragmentManager();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// instantiate superclass
		super.onCreate(savedInstanceState);

		//Init Realm database
		Realm.init(this);

		// Get a Realm instance for this thread
		realm = Realm.getDefaultInstance();

		//Init fuels
		FuelBO.getInstance();

		// get additional permissions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// Storage Permissions
			final int REQUEST_EXTERNAL_STORAGE = 1;
			final String[] PERMISSIONS_STORAGE = {
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
			};
			requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);


			// Workaround for FileUriExposedException in Android >= M
			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
			StrictMode.setVmPolicy(builder.build());
		}

		dlgBuilder = new AlertDialog.Builder(this);

		// get preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// register for later changes
		prefs.registerOnSharedPreferenceChangeListener(this);

		// Overlay feature has to be set before window content is set
		if (prefs.getBoolean(PREF_AUTOHIDE, false) && prefs.getBoolean(PREF_OVERLAY, false))
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		// Set up all data adapters
		mPidAdapter = new ObdItemAdapter(this, 0, ObdProt.PidPvs);
		mVidAdapter = new VidItemAdapter(this, 0, ObdProt.VidPvs);
		mDfcAdapter = new DfcItemAdapter(this, 0, ObdProt.tCodes);
		currDataAdapter = mPidAdapter;

		// update all settings from preferences
		onSharedPreferenceChanged(prefs, null);

		// Log program startup
		log.info(String.format("%s %s starting", getString(R.string.app_name), getString(R.string.app_version)));

		// set listeners for data structure changes
		setDataListeners();
		// automate elm status display
		CommService.elm.addPropertyChangeListener(this);

		//Init routeService
		routeService = RouteService.getInstance();
		routeService.init(this);

		// set content view
		setContentView(R.layout.startup_layout);

		//Create drawer menu
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		createDrawerMenu();

		//Create a fragment manager
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		NewRouteFragment fragment = new NewRouteFragment();
		fragmentTransaction.replace(R.id.frame_content, fragment);
		fragmentTransaction.commit();


		switch (CommService.medium)
		{
			case BLUETOOTH:
				// Get local Bluetooth adapter
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				log.fine("Adapter: " + mBluetoothAdapter);
				// If BT is not on, request that it be enabled.
				if (getMode() != MODE.DEMO && mBluetoothAdapter != null)
				{
					// remember initial bluetooth state
					initialBtStateEnabled = mBluetoothAdapter.isEnabled();
					if (!initialBtStateEnabled)
					{
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
					}
				}
				break;

		}
	}

	/**
	 * Handler for application start event
	 */
	@Override
	public void onStart()
	{
		super.onStart();

		// If the adapter is null, then Bluetooth is not supported
		if (CommService.medium == CommService.MEDIUM.BLUETOOTH && mBluetoothAdapter == null)
		{
			// start ELM protocol demo loop
			setMode(MODE.DEMO);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{

		try
		{
			// Reduce ELM power consumption by setting it to sleep
			CommService.elm.goToSleep();
			// wait until message is out ...
			Thread.sleep(100, 0);
		} catch (InterruptedException e)
		{
			// do nothing
			log.log(Level.FINER, e.getLocalizedMessage());
		}

		/* don't listen to ELM data changes any more */
		removeDataListeners();
		// don't listen to ELM property changes any more
		CommService.elm.removePropertyChangeListener(this);

		// stop demo service if it was started
		setMode(MODE.OFFLINE);

		// stop communication service
		if (mCommService != null) mCommService.stop();

		// if bluetooth adapter was switched OFF before ...
		if (mBluetoothAdapter != null && !initialBtStateEnabled)
		{
			// ... turn it OFF again
			mBluetoothAdapter.disable();
		}

		log.info(String.format("%s %s finished",
			getString(R.string.app_name),
			getString(R.string.app_version)));

		super.onDestroy();
	}

	@Override
	public void setContentView(int layoutResID)
	{
		setContentView(getLayoutInflater().inflate(layoutResID, null));
	}

	@Override
	public void setContentView(View view)
	{
		super.setContentView(view);
	}


	/**
	 * Create the drawer side menu
	 */
	private void createDrawerMenu(){
		PrimaryDrawerItem newRouteItem = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_menu_new_route).withIcon(R.drawable.add_1);
		PrimaryDrawerItem myRoutesItem = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.drawer_menu_my_routes).withIcon(R.drawable.placeholder);
		PrimaryDrawerItem myCarsItem = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.drawer_menu_my_cars).withIcon(R.drawable.car);

		//create the drawer and remember the `Drawer` result object
		drawerMenu = new DrawerBuilder()
				.withActivity(this)
				.withToolbar(toolbar)
				.addDrawerItems(
						newRouteItem,
						new DividerDrawerItem(),
						myRoutesItem,
						new DividerDrawerItem(),
						myCarsItem,
						new DividerDrawerItem()
				)
				.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
					@Override
					public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
						FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

						if(position == 0) {
							NewRouteFragment fragment = new NewRouteFragment();
							fragmentTransaction.replace(R.id.frame_content, fragment);
							fragmentTransaction.commit();

						}else if(position == 2){
							RouteListFragment fragment = new RouteListFragment();
							fragmentTransaction.replace(R.id.frame_content, fragment);
							fragmentTransaction.commit();

						}else if(position == 4){
							VehicleListFragment fragment = new VehicleListFragment();
							fragmentTransaction.replace(R.id.frame_content, fragment);
							fragmentTransaction.commit();
						}

						return false;
					}
				}).build();
	}

	/**
	 * handle pressing of the BACK-KEY
	 */
	@Override
	public void onBackPressed()
	{
		if (lastBackPressTime < System.currentTimeMillis() - EXIT_TIMEOUT){
			exitToast = Toast.makeText(this, R.string.back_again_to_exit, Toast.LENGTH_SHORT);
			exitToast.show();
			lastBackPressTime = System.currentTimeMillis();
		} else {
			if (exitToast != null){
				exitToast.cancel();
			}
			super.onBackPressed();
		}

	}

	/**
	 * Handler for options menu creation event
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// update menu item status for current conversion
		setConversionSystem(EcuDataItem.cnvSystem);
		return true;
	}


	/**
	 * Handler for Options menu selection
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked){}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode){}

	/**
	 * Handler for result messages from other activities
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		boolean secureConnection = false;

		switch (requestCode)
		{
			// device is connected
			case REQUEST_CONNECT_DEVICE_SECURE:
				secureConnection = true;
				// no break here ...
			case REQUEST_CONNECT_DEVICE_INSECURE:
				// When BtDeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{
					// Get the device MAC address
					String address = data.getExtras().getString(BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);

					// save reported address as last setting
					prefs.edit().putString(PRESELECT.LAST_DEV_ADDRESS.toString(), address).apply();
					connectBtDevice(address, secureConnection);
				} else
				{
					setMode(MODE.OFFLINE);
				}
				break;

			// bluetooth enabled
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK)
				{
					// Start online mode
					setMode(MODE.ONLINE);
				} else
				{
					// Start demo service Thread
					setMode(MODE.DEMO);
				}
				break;

			// settings finished
			case REQUEST_SETTINGS:
			break;

			// graphical data view finished
			case REQUEST_GRAPH_DISPLAY_DONE:
				break;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
	{
		// keep main display on?
		if (key==null || KEEP_SCREEN_ON.equals(key))
		{
			getWindow().addFlags(prefs.getBoolean(KEEP_SCREEN_ON, false)
								 ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
								 : 0);
		}

		// set default comm medium
		if(key==null || SettingsActivity.KEY_COMM_MEDIUM.equals(key))
			CommService.medium = CommService.MEDIUM.values()[getPrefsInt(SettingsActivity.KEY_COMM_MEDIUM, 0)];

		// enable/disable ELM adaptive timing
		if(key==null || ELM_ADAPTIVE_TIMING.equals(key))
			CommService.elm.mAdaptiveTiming.setMode(
				ElmProt.AdaptTimingMode.valueOf(
					prefs.getString(ELM_ADAPTIVE_TIMING,
						ElmProt.AdaptTimingMode.OFF.toString())));

		// set protocol flag to initiate immediate reset on NRC reception
		if(key==null || ELM_RESET_ON_NRC.equals(key))
			CommService.elm.setResetOnNrc(prefs.getBoolean(ELM_RESET_ON_NRC, false));

		// set custom ELM init commands
		if(key==null || ELM_CUSTOM_INIT_CMDS.equals(key))
		{
			String value = prefs.getString(ELM_CUSTOM_INIT_CMDS, null);
			if(value != null && value.length() > 0)
				CommService.elm.setCustomInitCommands(value.split("\n"));
		}

		// ELM timeout
		if(key==null || SettingsActivity.ELM_MIN_TIMEOUT.equals(key))
			CommService.elm.mAdaptiveTiming.setElmTimeoutMin(
				getPrefsInt(SettingsActivity.ELM_MIN_TIMEOUT,
					CommService.elm.mAdaptiveTiming.getElmTimeoutMin()));

		// ... measurement system
		if(key==null || MEASURE_SYSTEM.equals(key))
			setConversionSystem(getPrefsInt(MEASURE_SYSTEM, EcuDataItem.SYSTEM_METRIC));

		// ... preferred protocol
		if(key==null || SettingsActivity.KEY_PROT_SELECT.equals(key))
			ElmProt.setPreferredProtocol(getPrefsInt(SettingsActivity.KEY_PROT_SELECT, 0));

		// set disabled ELM commands
		if(key==null || SettingsActivity.ELM_CMD_DISABLE.equals(key))
		{
			ElmProt.disableCommands(prefs.getStringSet(SettingsActivity.ELM_CMD_DISABLE, null));
		}

		// Max. data disabling debounce counter
		if(key==null || PREF_DATA_DISABLE_MAX.equals(key))
			EcuDataItem.MAX_ERROR_COUNT = getPrefsInt(PREF_DATA_DISABLE_MAX, 3);
	}



	/**
	 * Handler for PV change events This handler just forwards the PV change
	 * events to the android handler, since all adapter / GUI actions have to be
	 * performed from the main handler
	 *
	 * @param event PvChangeEvent which is reported
	 */
	@Override
	public synchronized void pvChanged(PvChangeEvent event)
	{
		// forward PV change to the UI Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DATA_ITEMS_CHANGED);
		if(!event.isChildEvent())
		{
			msg.obj = event;
			mHandler.sendMessage(msg);
		}
	}

	/**
	 * Handle message requests
	 */
	private transient final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			try
			{
				PropertyChangeEvent evt;

				RealmQuery<ObdData> query = realm.where(ObdData.class);
				RealmResults<ObdData> result1 = query.findAll();

				// log trace message for received handler notification event
				log.log(Level.FINEST, String.format("Handler notification: %s", msg.toString()));

				switch (msg.what)
				{
					case MESSAGE_STATE_CHANGE:
						// log trace message for received handler notification event
						log.log(Level.FINEST, String.format("State change: %s", msg.toString()));
						switch ((CommService.STATE) msg.obj)
						{
							case CONNECTED:
								onConnect();
								break;

							case CONNECTING:
								setStatus(R.string.title_connecting);
								break;

							default:
								onDisconnect();
								break;
						}
						break;

					case MESSAGE_FILE_WRITTEN:
						break;

					case MESSAGE_DEVICE_NAME:
						// save the connected device's name
						mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
						Toast.makeText(getApplicationContext(),	getString(R.string.connected_to) + mConnectedDeviceName,	Toast.LENGTH_SHORT).show();

						Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_content);
						if (currentFragment instanceof NewRouteFragment){
							((NewRouteFragment)currentFragment).updateView(NewRouteFragment.VIEW.CONNECTED);
						}

						break;

					case MESSAGE_TOAST:
					//	Toast.makeText(getApplicationContext(),
					//		msg.getData().getString(TOAST),
					//		Toast.LENGTH_SHORT).show();
						break;

					case MESSAGE_DATA_ITEMS_CHANGED:
						PvChangeEvent event = (PvChangeEvent) msg.obj;
						switch (event.getType())
						{
							case PvChangeEvent.PV_ADDED:
								currDataAdapter.setPvList(currDataAdapter.pvs);
								try
								{
									// set up data update timer
									updateTimer.schedule(updateTask, 0, DISPLAY_UPDATE_TIME);
								} catch (Exception ignored)
								{
									log.log(Level.FINER, "Error adding PV", ignored);
								}
								break;

							case PvChangeEvent.PV_CLEARED:
								currDataAdapter.clear();
								break;
						}
						break;

					case MESSAGE_UPDATE_VIEW:

						if(currDataAdapter instanceof VidItemAdapter){
							routeService.saveCarInformation(currDataAdapter.pvs);

						}else if(currDataAdapter instanceof ObdItemAdapter){
							filterData(true);
							routeService.saveRouteData(currDataAdapter.pvs);
						}

						break;

					// handle state change in OBD protocol
					case MESSAGE_OBD_STATE_CHANGED:
						evt = (PropertyChangeEvent) msg.obj;
						ElmProt.STAT state = (ElmProt.STAT)evt.getNewValue();
                        /* Show ELM status only in ONLINE mode */
						if (getMode() != MODE.DEMO)
						{
							setStatus(getResources().getStringArray(R.array.elmcomm_states)[state.ordinal()]);
							if(state.name().equals("NODATA") && currDataAdapter instanceof VidItemAdapter){
								routeService.setVehicleInformationAvailable(false);
							}
						}
						// if last selection shall be restored ...
						if(istRestoreWanted(PRESELECT.LAST_SERVICE))
						{
							if(state == ElmProt.STAT.ECU_DETECTED)
							{
								setObdService(prefs.getInt(PRESELECT.LAST_SERVICE.toString(),0), null);
							}
						}
						break;

					// handle change in number of fault codes
					case MESSAGE_OBD_NUMCODES:
						break;

					// handle ECU detection event
					case MESSAGE_OBD_ECUS:
						evt = (PropertyChangeEvent) msg.obj;
						selectEcu((Set<Integer>) evt.getNewValue());
						break;

					// handle negative result code from OBD protocol
					case MESSAGE_OBD_NRC:
						// reset OBD mode to prevent infinite error loop
						setObdService(ObdProt.OBD_SVC_NONE, getText(R.string.obd_error));
						// show error dialog ...
						evt = (PropertyChangeEvent) msg.obj;
						String nrcMessage = (String)evt.getNewValue();
						dlgBuilder
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setTitle(R.string.obd_error)
							.setMessage(nrcMessage)
							.setPositiveButton(null,null)
							.show();
						break;

					// set toolbar visibility
					case MESSAGE_TOOLBAR_VISIBLE:
						Boolean visible = (Boolean)msg.obj;
						// log action
						log.fine(String.format("ActionBar: %s", visible ? "show" : "hide"));
						// set action bar visibility
						ActionBar ab = getActionBar();
						if(ab != null)
						{
							if(visible)
							{
								ab.show();
							} else
							{
								ab.hide();
							}
						}
						break;
				}
			}
			catch(Exception ex)
			{
				log.log(Level.SEVERE, "Error in mHandler", ex);
			}
		}
	};

	/**
	 * Check if restore of specified preselection is wanted from settings
	 * @param preselect specified preselect
	 * @return flag if preselection shall be restored
	 */
	boolean istRestoreWanted(PRESELECT preselect)
	{
		return prefs.getStringSet(PREF_USE_LAST, emptyStringSet).contains(preselect.toString());
	}


	/**
	 * Prompt for selection of a single ECU from list of available ECUs
	 * @param ecuAdresses List of available ECUs
	 */
	protected void selectEcu(final Set<Integer> ecuAdresses)
	{
		// if more than one ECUs available ...
		if(ecuAdresses.size() > 1)
		{
			int preferredAddress = prefs.getInt(PRESELECT.LAST_ECU_ADDRESS.toString(), 0);
			// check if last preferred address matches any of the reported addresses
			if(istRestoreWanted(PRESELECT.LAST_ECU_ADDRESS)
			   && ecuAdresses.contains(preferredAddress))
			{
				// set addrerss
				CommService.elm.setEcuAddress(preferredAddress);
			}
			else
			{
				// NO match with preference -> allow selection

				// .. allow selection of single ECU address ...
				final CharSequence[] entries = new CharSequence[ecuAdresses.size()];
				// create list of entries
				int i = 0;
				for (Integer addr : ecuAdresses)
				{
					entries[i++] = String.format("0x%X", addr);
				}
				// show dialog ...
				dlgBuilder
					.setTitle(R.string.select_ecu_addr)
					.setItems(entries, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							int address = Integer.parseInt(entries[which].toString().substring(2), 16);
							// set address
							CommService.elm.setEcuAddress(address);
							// set this as preference (preference change will trigger ELM command)
							prefs.edit().putInt(PRESELECT.LAST_ECU_ADDRESS.toString(), address).apply();
						}
					}).show();
			}
		}
	}


	/**
	 * Timer Task to cyclically update data screen
	 */
	private transient final TimerTask updateTask = new TimerTask()
	{
		@Override
		public void run()
		{
			/* forward message to update the view */
			Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_UPDATE_VIEW);
			mHandler.sendMessage(msg);
		}
	};


	/**
	 * Get preference int value
	 *
	 * @param key preference key name
	 * @param defaultValue numeric default value
	 *
	 * @return preference int value
	 */
	private int getPrefsInt(String key, int defaultValue)
	{
		int result = defaultValue;

		try
		{
			result = Integer.valueOf(prefs.getString(key, String.valueOf(defaultValue)));
		}
		catch( Exception ex)
		{
			// log error message
			log.severe(String.format("Preference '%s'(%d): %s", key, result, ex.toString()));
		}

		return result;
	}

	/**
	 * set listeners for data structure changes
	 */
	private void setDataListeners()
	{
		// add pv change listeners to trigger model updates
		ObdProt.PidPvs.addPvChangeListener(this,PvChangeEvent.PV_ADDED | PvChangeEvent.PV_CLEARED);
		ObdProt.VidPvs.addPvChangeListener(this,PvChangeEvent.PV_ADDED | PvChangeEvent.PV_CLEARED);
		ObdProt.tCodes.addPvChangeListener(this,PvChangeEvent.PV_ADDED | PvChangeEvent.PV_CLEARED);
	}

	/**
	 * set listeners for data structure changes
	 */
	private void removeDataListeners()
	{
		// remove pv change listeners
		ObdProt.PidPvs.removePvChangeListener(this);
		ObdProt.VidPvs.removePvChangeListener(this);
		ObdProt.tCodes.removePvChangeListener(this);
	}

	MODE previousMode;

	/**
	 * get current operating mode
	 */
	public MODE getMode()
	{
		return mode;
	}

	/**
	 * set new operating mode
	 *
	 * @param mode new mode
	 */
	public void setMode(MODE mode)
	{
		// if this is a mode change, or file reload ...
		if (mode != this.mode || mode == MODE.FILE)
		{
			switch (mode)
			{
				case OFFLINE:
					Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_content);
					if (currentFragment instanceof NewRouteFragment){
						((NewRouteFragment)currentFragment).updateView(NewRouteFragment.VIEW.NOT_CONNECTED);
					}
					break;

				case ONLINE:
					switch (CommService.medium)
					{
						case BLUETOOTH:
							// if pre-settings shall be used ...
							String address = prefs.getString(PRESELECT.LAST_DEV_ADDRESS.toString(), null);
							if(istRestoreWanted(PRESELECT.LAST_DEV_ADDRESS) && address != null) {

								// ... connect with previously connected device
								connectBtDevice(address, prefs.getBoolean("bt_secure_connection", false));
							} else {
								// ... otherwise launch the BtDeviceListActivity to see devices and do scan
								Intent serverIntent = new Intent(this, BtDeviceListActivity.class);
								startActivityForResult(serverIntent, prefs.getBoolean("bt_secure_connection", false) ? REQUEST_CONNECT_DEVICE_SECURE	: REQUEST_CONNECT_DEVICE_INSECURE );
							}
							break;
					}
					break;

			}
			// remember previous mode
			previousMode = this.mode;
			// set new mode
			this.mode = mode;
			setStatus(mode.toString());
		}
	}

	/**
	 * set mesaurement conversion system to metric/imperial
	 *
	 * @param cnvId ID for metric/imperial conversion
	 */
	void setConversionSystem(int cnvId)
	{
		log.info("Conversion: " + getResources().getStringArray(R.array.measure_options)[cnvId]);
		if (EcuDataItem.cnvSystem != cnvId)
		{
			// set conversion system
			EcuDataItem.cnvSystem = cnvId;
		}
	}


	/**
	 * set status message in status bar
	 *
	 * @param resId Resource ID of the text to be displayed
	 */
	private void setStatus(int resId)
	{
		setStatus(getString(resId));
	}

	/**
	 * set status message in status bar
	 *
	 * @param subTitle status text to be set
	 */
	private void setStatus(CharSequence subTitle)
	{
		if (toolbar != null){
			toolbar.setSubtitle(subTitle);
		}
	}

	/**
	 * Initiate a connect to the selected bluetooth device
	 *
	 * @param address bluetooth device address
	 * @param secure flag to indicate if the connection shall be secure, or not
	 */
	private void connectBtDevice(String address, boolean secure)
	{
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mCommService = new BtCommService(this, mHandler);
		mCommService.connect(device, secure);
	}


	/**
	 * Activate desired OBD service
	 *
	 * @param newObdService OBD service ID to be activated
	 */
	public void setObdService(int newObdService, CharSequence menuTitle)
	{

		// set title
		ActionBar ab = getActionBar();
		if (ab != null)
		{
			// title specified ... show it
			if (menuTitle != null)
			{
				toolbar.setTitle(menuTitle);
			} else
			{
				// no title specified, set to app name if no service set
				if (newObdService == ElmProt.OBD_SVC_NONE)
				{
					toolbar.setTitle(getString(R.string.app_name));
				}
			}
		}
		// set protocol service
		CommService.elm.setService(newObdService, (getMode() != MODE.FILE));

		// set corresponding list adapter
		switch (newObdService)
		{
			case ObdProt.OBD_SVC_DATA:
				// no break here
			case ObdProt.OBD_SVC_FREEZEFRAME:
				currDataAdapter = mPidAdapter;
				break;

			case ObdProt.OBD_SVC_PENDINGCODES:
			case ObdProt.OBD_SVC_PERMACODES:
			case ObdProt.OBD_SVC_READ_CODES:
				currDataAdapter = mDfcAdapter;
				break;

			case ObdProt.OBD_SVC_NONE:
				// intentionally no break to initialize adapter
			case ObdProt.OBD_SVC_VEH_INFO:
				currDataAdapter = mVidAdapter;
				break;
		}
		// remember this as last selected service
		if(newObdService > ObdProt.OBD_SVC_NONE)
			prefs.edit().putInt(PRESELECT.LAST_SERVICE.toString(), newObdService).apply();
	}


	/**
	 * Handle bluetooth connection established ...
	 */
	public void onConnect()
	{
		//stopDemoService();

		mode = MODE.ONLINE;
		// display connection status
		setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
		// send RESET to Elm adapter
		CommService.elm.reset();
	}

	/**
	 * Handle bluetooth connection lost ...
	 */
	public void onDisconnect()
	{
		// handle further initialisations
		setMode(MODE.OFFLINE);
		Toast.makeText(this,getString(R.string.main_activity_disconnected),Toast.LENGTH_SHORT).show();
	}

	/**
	 * Property change listener to ELM-Protocol
	 *
	 * @param evt the property change event to be handled
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
    /* handle protocol status changes */
		if (ElmProt.PROP_STATUS.equals(evt.getPropertyName()))
		{
			// forward property change to the UI Activity
			Message msg = mHandler.obtainMessage(MESSAGE_OBD_STATE_CHANGED);
			msg.obj = evt;
			mHandler.sendMessage(msg);
		} else if (ElmProt.PROP_NUM_CODES.equals(evt.getPropertyName()))
		{
			// forward property change to the UI Activity
			Message msg = mHandler.obtainMessage(MESSAGE_OBD_NUMCODES);
			msg.obj = evt;
			mHandler.sendMessage(msg);
		} else if (ElmProt.PROP_ECU_ADDRESS.equals(evt.getPropertyName()))
		{
			// forward property change to the UI Activity
			Message msg = mHandler.obtainMessage(MESSAGE_OBD_ECUS);
			msg.obj = evt;
			mHandler.sendMessage(msg);
		} else if (ObdProt.PROP_NRC.equals(evt.getPropertyName()))
		{
			// forward property change to the UI Activity
			Message msg = mHandler.obtainMessage(MESSAGE_OBD_NRC);
			msg.obj = evt;
			mHandler.sendMessage(msg);
		}
	}


	public CommService.STATE getConnectionState(){
		return mCommService==null ? null : mCommService.getState();
	}

	public NewRouteFragment getMapFragment(){
		Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_content);
		if (currentFragment instanceof NewRouteFragment){
			return (NewRouteFragment) currentFragment;
		}
		return null;
	}

	/**
	 * Filter display items to just the necessary ones
	 */
	public void filterData(boolean filtered)
	{
		if (filtered)
		{
			PvList filteredList = new PvList();
			TreeSet<Integer> selPids = new TreeSet<Integer>();

			for(Object ed: mPidAdapter.pvs.keySet()) {
				EcuDataPv ecuDataPv = (EcuDataPv) mPidAdapter.pvs.get(ed);
				if ("engine_speed".equals(ecuDataPv.get("MNEMONIC"))) { //PID=12
					selPids.add(ecuDataPv.getAsInt(EcuDataPv.FID_PID));
					filteredList.put(ecuDataPv.toString(), ecuDataPv);
				}

				else if ("vehicle_speed".equals(ecuDataPv.get("MNEMONIC"))) { //PID=13
					selPids.add(ecuDataPv.getAsInt(EcuDataPv.FID_PID));
					filteredList.put(ecuDataPv.toString(), ecuDataPv);
				}

				else if ("mass_airflow".equals(ecuDataPv.get("MNEMONIC"))) { //PID=16
					selPids.add(ecuDataPv.getAsInt(EcuDataPv.FID_PID));
					filteredList.put(ecuDataPv.toString(), ecuDataPv);
				}

				else if ("o2_sensor_lambda_b1s1".equals(ecuDataPv.get("MNEMONIC"))) { //PID=17
					selPids.add(ecuDataPv.getAsInt(EcuDataPv.FID_PID));
					filteredList.put(ecuDataPv.toString(), ecuDataPv);
				}
			}

			if(!filteredList.isEmpty()) {
				mPidAdapter.setPvList(filteredList);
				setFixedPids(selPids);
			}
		} else
		{
			ObdProt.resetFixedPid();
			mPidAdapter.setPvList(ObdProt.PidPvs);
		}
	}

	public static void setFixedPids(Set<Integer> pidNumbers)
	{
		int[] pids = new int[pidNumbers.size()];
		int i = 0;
		for (Integer pidNum : pidNumbers) pids[i++] = pidNum;
		Arrays.sort(pids);
		// set protocol fixed PIDs
		ObdProt.setFixedPid(pids);
	}



	public Drawer getDrawerMenu() {
		return drawerMenu;
	}

}
