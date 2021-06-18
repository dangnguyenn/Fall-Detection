//global variables
var selected_user = null;
var userDocId = null;
var defaultActivities = ["Walking", "Drinking", "Reading"];
var currentActivities = null;
var activityDocId = null;
var json_objects = null;

exports.setup = function() {
    //uni-directional UI-database
    this.output('documentSend');
    //bi-directional UI-database : profile
    this.input('profilesReceive');
    this.output('loadSend', {spontaneous: true, 'type': 'string'});
    //uni-directional UI-dataCollection
    this.output('activitySend');
    //bi-directional UI-database : activity
    this.input('activityReceive');
    this.output('getActivitySend', {spontaneous: true, 'type': 'string'});
    //uni-directional UI-database
    this.output('updatedDocumentSend');

    currentActivities = null;
    activityDocId = null;
};

exports.initialize = function() {
    var self = this;
    this.addInputHandler('profilesReceive', loadProfile.bind(this));
    this.addInputHandler('activityReceive', getActivityList.bind(this));

    var createBtnLogin = document.getElementById("create-button-login");
    createBtnLogin.onclick = function() {
        var databaseName = "userDatabase";
        self.send('loadSend', databaseName);    //get all profiles in database for validation
    };

    var name = document.getElementById("create-username");
    name.onchange = function() {
        var isValidName = true;
        json_objects.forEach((user) => {
            if (name.value === user.keys.userName || name.value === "User ") {
                isValidName = false;
            }
        });
        if (!isValidName) {
            name.setCustomValidity('user name already exists');
            document.getElementById("invalid-username").innerHTML = "User Name already exists";
        } else {
            name.setCustomValidity('');
            document.getElementById("invalid-username").innerHTML = "";
        }
    };

    var createBtn = document.getElementById("create-button");
    createBtn.onclick = () => {
        var isValidForm = true;
        var forms = document.getElementsByClassName('needs-validation');

        var validation = Array.prototype.filter.call(forms, function(form) {
            if (form.checkValidity() === false) {
                isValidForm = false;
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });

        if (isValidForm) {
            createProfile(self);
            alert("You have successfully created new profile.\n\nPlease Load profile to continue.")
//            document.getElementById('index-screen').style.display = "none";
//            document.getElementById('home-screen').style.display = "block";
        }
    };

    var loadProfileBtn = document.getElementById("load_button_login");
    loadProfileBtn.onclick = function() {
        var databaseName = "userDatabase";
        self.send('loadSend', databaseName);    //get all profiles in database for dropdown list
    }


    var signOutBtn = document.getElementById("sign-out-button");
    signOutBtn.onclick = function() {

        var activities = {
            "uuid": selected_user.uuid,
            "activities": currentActivities
        }
        if (activityDocId == null) {
            var doc = {"databaseName": "activityDatabase", "document": activities};
            self.send('documentSend', doc);     //insert the new activity list to database
        } else {
            var doc = {"databaseName": "activityDatabase", "documentId": activityDocId, "updatedDocument": activities};
            self.send('updatedDocumentSend', doc);     //update activity list on database
        }

        currentActivities = null;
        activityDocId = null;

        document.getElementById("create-profile-form").reset();
        var forms = document.getElementsByClassName('needs-validation');
        var validation = Array.prototype.filter.call(forms, function(form) {
            form.classList.remove('was-validated');
        });
        document.getElementById("activity-tab").classList.add('active');
        document.getElementById("activity").classList.add('active');
        document.getElementById("sign-out-tab").classList.remove('active');
        document.getElementById("sign-out").classList.remove('active');

        document.getElementById("activity-list-button").innerText = "Activity";
        document.getElementById("dropdownMenuButton").innerText = "User ";
        document.getElementById("add-activity").value = "";

        document.getElementById('index-screen').style.display = "block";
        document.getElementById('home-screen').style.display = "none";
    }

    var activityBtnDiv = document.getElementById("activity-button-div");
    var sendActivityBtn = document.getElementById("send-activity-button");
    sendActivityBtn.onclick = function() {
            var message = {"message":"start"};
            self.send('activitySend', message);    //send activity to the watch

    }
};

function createProfile(self) {
    var userUUID = uuidv4();
    var userName = document.getElementById("create-username").value;
    var userDob = document.getElementById("create-dob").value;
    var userEmail = document.getElementById("create-email").value;
    var userPhone = document.getElementById("create-telephone").value;
    var sexOptions = document.getElementsByName('sex-option');
    var userSex = null;
    sexOptions.forEach(sex => {
        if(sex.checked) userSex = sex.value;
    })
    var userHeightFt = document.getElementById("create-height-ft").value;
    var userHeightIn = document.getElementById("create-height-in").value;
    var userWeight = document.getElementById("create-weight").value;
    var carerName = document.getElementById("create-carer-name").value;
    var carerEmail = document.getElementById("create-carer-email").value;
    var carerPhone = document.getElementById("create-carer-telephone").value;

    var profile = {
        "uuid": userUUID,
        "userName": userName,
        "userDob": userDob,
        "userEmail": userEmail,
        "userPhone": userPhone,
        "userSex": userSex,
        "userHeightFt": userHeightFt,
        "userHeightIn": userHeightIn,
        "userWeight": userWeight,
        "carerName": carerName,
        "carerEmail": carerEmail,
        "carerPhone": carerPhone
    };
    selected_user = profile;
    displayProfile(self);

    var doc = {"databaseName": "userDatabase", "document": profile};
    self.send('documentSend', doc);     //insert the profile to database

    currentActivities = defaultActivities;
    updateActivityList();
}

function loadProfile() {
    var div = document.getElementById('user-list');
    while(div.firstChild) {
           div.removeChild(div.firstChild);
    }

    json_objects = this.get('profilesReceive');
    json_objects.forEach((user) => {
        var div = document.getElementById('user-list');
        var item = document.createElement('button');
        var name = user.keys.userName;
        item.innerText = name;
        item.setAttribute('class', 'dropdown-item');
        item.onclick = () => {
            document.getElementById("dropdownMenuButton").innerText = name;
            selected_user = user.keys;
            userDocId = user.id;
        }
        div.appendChild(item);
    });

    var loadBtn = document.getElementById('close-load');
    loadBtn.onclick = () => {
        var activityDatabaseName = "activityDatabase";
        this.send('getActivitySend', activityDatabaseName);     //get all activity lists in database (each profile has 1 activity list)

        if (document.getElementById("dropdownMenuButton").innerText !== "User ") {
            document.getElementById('index-screen').style.display = "none";
            document.getElementById('home-screen').style.display = "block";
            displayProfile(this);
        }
    }
}

function displayProfile(self) {
    var profile = selected_user;
    var profile_div = document.getElementById('profile');
    var profile_button = document.getElementById('profile-tab');
    profile_button.onclick = function() {
        while(profile_div.firstChild) {
            profile_div.removeChild(profile_div.firstChild);
        }
        var header = document.createElement('h4');
        header.setAttribute('class', 'd-flex justify-content-center mt-4 mb-3');
        header.setAttribute('style', 'color: white');
        header.innerText = "User information";
        profile_div.appendChild(header);

        var item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Name: " + profile.userName;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User DOB: " + profile.userDob;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Email: " + profile.userEmail;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Phone Number: " + profile.userPhone;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Sex: " + profile.userSex;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Height: " + profile.userHeightFt + " ft " + profile.userHeightIn + " in";
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "User Weight: " + profile.userWeight + " lbs";
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "Carer Name: " + profile.carerName;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "Carer Email: " + profile.carerEmail;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "Carer Phone Number: " + profile.carerPhone;
        profile_div.appendChild(item);
        item = document.createElement('p');
        item.setAttribute('style','color: white');
        item.innerHTML = "UUID: " + profile.uuid;
        profile_div.appendChild(item);

        var edit_profile_div = document.createElement('div');
        edit_profile_div.setAttribute('class', 'd-flex justify-content-center');
        var edit_profile_button = document.createElement('button');
        edit_profile_button.setAttribute('type', 'button');
        edit_profile_button.setAttribute('class', 'btn btn-lg mt-4');
        edit_profile_button.setAttribute('style', 'background: #501214; color: white');
        edit_profile_button.setAttribute('data-toggle','modal');
        edit_profile_button.setAttribute('data-target','#edit-profile-modal');
        edit_profile_button.innerText = 'Edit Profile';
        edit_profile_div.appendChild(edit_profile_button);
        profile_div.appendChild(edit_profile_div);

        edit_profile_button.onclick = () => {
            document.getElementById('edit-create-username').value = profile.userName;
            document.getElementById('edit-create-username').readOnly = true;
            document.getElementById('edit-create-dob').value = profile.userDob;
            document.getElementById('edit-create-email').value = profile.userEmail;
            document.getElementById('edit-create-telephone').value = profile.userPhone;
            if (profile.userSex === 'Male') {
                document.getElementById('edit-sex-male').checked = true;
            } else {
                document.getElementById('edit-sex-female').checked = true;
            }
            document.getElementById('edit-create-height-ft').value = profile.userHeightFt;
            document.getElementById('edit-create-height-in').value = profile.userHeightIn;
            document.getElementById('edit-create-weight').value = profile.userWeight;
            document.getElementById('edit-create-carer-name').value = profile.carerName;
            document.getElementById('edit-create-carer-email').value = profile.carerEmail;
            document.getElementById('edit-create-carer-telephone').value = profile.carerPhone;
        }

        var editBtn = document.getElementById("edit-create-button");
        editBtn.onclick = () => {
            var isValidForm = true;
            var forms = document.getElementsByClassName('edit-needs-validation');

            var validation = Array.prototype.filter.call(forms, function(form) {
                if (form.checkValidity() === false) {
                    isValidForm = false;
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            });

            if (isValidForm) {
                editProfile(self);
                document.getElementById('index-screen').style.display = "none";
                document.getElementById('home-screen').style.display = "block";
            }
        };
    };
}

function editProfile(self) {
    var userName = document.getElementById("edit-create-username").value;
    var userDob = document.getElementById("edit-create-dob").value;
    var userEmail = document.getElementById("edit-create-email").value;
    var userPhone = document.getElementById("edit-create-telephone").value;
    var sexOptions = document.getElementsByName('edit-sex-option');
    var userSex = null;
    sexOptions.forEach(sex => {
        if(sex.checked) userSex = sex.value;
    })
    var userHeightFt = document.getElementById("edit-create-height-ft").value;
    var userHeightIn = document.getElementById("edit-create-height-in").value;
    var userWeight = document.getElementById("edit-create-weight").value;
    var carerName = document.getElementById("edit-create-carer-name").value;
    var carerEmail = document.getElementById("edit-create-carer-email").value;
    var carerPhone = document.getElementById("edit-create-carer-telephone").value;

    var updatedProfile = {
        "uuid": selected_user.uuid,
        "userName": userName,
        "userDob": userDob,
        "userEmail": userEmail,
        "userPhone": userPhone,
        "userSex": userSex,
        "userHeightFt": userHeightFt,
        "userHeightIn": userHeightIn,
        "userWeight": userWeight,
        "carerName": carerName,
        "carerEmail": carerEmail,
        "carerPhone": carerPhone
    };

    var updatedField = { }

    for (i in updatedProfile) {
        for (j in selected_user) {
            if (i === j && updatedProfile[i] !== selected_user[j]) {
                var key = i;
                updatedField[key] = updatedProfile[i];
            }
        }
    }

    selected_user = updatedProfile;
    displayProfile(self);
    document.getElementById('profile-tab').click();

    var data = {
        "databaseName": "userDatabase",
        "documentId": userDocId,
        "updatedDocument": updatedField
    };
    self.send('updatedDocumentSend', data);     //update the profile on database
}

function getActivityList() {
    var self = this;
    var all_activities = self.get('activityReceive');
    all_activities.forEach((activity) => {
        if(activity.keys.uuid == selected_user.uuid) {
            currentActivities = activity.keys.activities;
            activityDocId = activity.id;
        }
    });
    updateActivityList();
}

function updateActivityList() {
    var activityBtn = document.getElementById('activity-list-button');
    var div = document.getElementById('activity-list');
    var selected_activity = null;
    activityBtn.onclick = function() {
        while(div.firstChild) {
            div.removeChild(div.firstChild);
        }
        currentActivities.forEach((activity) => {
            var item = document.createElement('button');
            item.innerText = activity;
            item.setAttribute('class', 'dropdown-item');
            item.onclick = () => {
                document.getElementById('activity-list-button').innerText = activity;
                selected_activity = activity;
            }
            div.appendChild(item);
        });
        var divider = document.createElement('div');
        divider.setAttribute('class', 'dropdown-divider');
        divider.setAttribute('id', 'divider');
        div.appendChild(divider);
        var add_activity = document.createElement('button');
        add_activity.setAttribute('class', 'dropdown-item');
        add_activity.setAttribute('type', 'button');
        add_activity.setAttribute('data-toggle', 'modal');
        add_activity.setAttribute('data-target', '#add-activity-modal');
        add_activity.innerText = 'Add new activity';
        div.appendChild(add_activity);
    };

    var addActivity = document.getElementById('add-activity');
    var addActivityBtn = document.getElementById('add-activity-button');
    addActivityBtn.onclick = function() {
        currentActivities.push(addActivity.value);
        document.getElementById("activity-list-button").innerText = addActivity.value;
    }
}

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}