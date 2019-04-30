import {app, BrowserWindow, Menu} from "electron";
import MenuItemConstructorOptions = Electron.MenuItemConstructorOptions;

const contextMenu = require("electron-context-menu");
const debug = require('electron-debug');

debug();

let mainWindow: BrowserWindow | null;

const isDevelopment = process.env.NODE_ENV !== 'production';

contextMenu();

const appMenu: MenuItemConstructorOptions = {
  label: "Application",//this name is set by project name in package.json on macos.
  role: "appMenu",
  submenu: [
    {label: "About", role: "about"},
    {label: 'Quit', role: 'quit'}
  ]
};

const editMenu: MenuItemConstructorOptions = {
  label: "Edit",
  role: "editMenu"
};

const devMenu: MenuItemConstructorOptions = {
  label: 'Development',
  submenu: [
    {label: 'Reload', role: 'reload'},
    {label: 'ForceReload', role: 'forceReload'},
    {label: 'Toggle DevTools', role: 'toggleDevTools'},
    {label: 'Quit', role: 'quit'}
  ]
};

function createWindow() {
  // Create the browser window.
  mainWindow = new BrowserWindow({
    height: 800,
    width: 1024,
    webPreferences: {
      nodeIntegration: true,
      devTools: true,

    }
  });

  const url = isDevelopment
    ? `http://localhost:${process.env.ELECTRON_WEBPACK_WDS_PORT}`
    : `file://${__dirname}/index.html`

  mainWindow.loadURL(url)

  mainWindow.webContents.on('devtools-opened', () => {
    mainWindow!.focus()
    setImmediate(() => {
      mainWindow!.focus()
    })
  });

  // Emitted when the window is closed.
  mainWindow.on("closed", () => {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    mainWindow = null;
  });

  // Create the Application's main menu
  let template: MenuItemConstructorOptions[] = [appMenu, editMenu];
  if (isDevelopment) {
    template.push(devMenu);
  }

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on("ready", createWindow);

// Quit when all windows are closed.
app.on("window-all-closed", () => {
  // On OS X it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("activate", () => {
  // On OS X it"s common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (mainWindow === null) {
    createWindow();
  }
});

// In this file you can include the rest of your app"s specific main process
// code. You can also put them in separate files and require them here.

// SSL/TSL: this is the self signed certificate support
app.on('certificate-error', (event, webContents, url, error, certificate, callback) => {
  // On certificate error we disable default behaviour (stop loading the page)
  // and we then say "it is all fine - true" to the callback
  event.preventDefault();
  callback(true);
});
