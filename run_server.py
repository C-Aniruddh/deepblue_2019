from flask import Flask, render_template, url_for, request, session, redirect, send_from_directory, jsonify
import os
from flask_pymongo import PyMongo
import bcrypt
import datetime
import json
import timeago
from geopy.geocoders import Nominatim
from bson.json_util import dumps

from processor import Processor
from doc_processor import DocumentManager

from werkzeug.utils import secure_filename
import shutil

app = Flask(__name__)
APP_ROOT = os.path.dirname(os.path.abspath(__file__))
DOWNLOAD_FOLDER = os.path.join(APP_ROOT, 'static/downloads')
UPLOAD_FOLDER = os.path.join(APP_ROOT, 'static/uploads')
PROCESSED_FOLDER = os.path.join(APP_ROOT, 'static/processed')
FINAL_DOC_FOLDER = os.path.join(APP_ROOT, 'static/documents')

app.config['MONGO_DBNAME'] = 'csp'
app.config['MONGO_URI'] = 'mongodb://localhost:27017/csp'
app.config['DOWNLOAD_FOLDER'] = DOWNLOAD_FOLDER
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['PROCESSED_FOLDER'] = PROCESSED_FOLDER
app.config['FINAL_DOC_FOLDER'] = FINAL_DOC_FOLDER


app.secret_key = 'mysecret'

ALLOWED_EXTENSIONS = {'jpg', 'png', 'jpeg', 'zip'}

mongo = PyMongo(app)

"""
    Snippets : url_for('static', filename='path/to/file')
"""

# Main App

@app.route('/')
def index():
    '''
        Index
    '''
    if 'username' in session:
        users = mongo.db.users
        issues = mongo.db.issues
        references = mongo.db.references
        rankings = mongo.db.rankings

        find_all_users = users.find({})
        total_users = find_all_users.count()

        find_references = references.find({})
        total_references = find_references.count()

        find_issues = issues.find({})
        total_issues = find_issues.count()

        find_top_rank = rankings.find_one({'rank' : 1})
        top_issue_id = find_top_rank['issue_id']

        return render_template('pages/app/dashboard.html', total_issues=total_issues, total_references=total_references, 
            total_users=total_users, top_issue_id=top_issue_id)
    else:
        return redirect('/userlogin')


@app.route('/app/issues/create', methods=['POST', 'GET'])
def create_issue():
    if 'username' in session:

        issues = mongo.db.issues
        documents = mongo.db.documents
        progress = mongo.db.progress
        references = mongo.db.references
        users = mongo.db.users

        if request.method == 'POST':
            image_one = request.files['image_one']
            image_two = request.files['image_two']
            if (image_one and allowed_file(image_two.filename) and (image_two and allowed_file(image_two.filename))):
                image_one_filename = secure_filename(image_one.filename)
                image_two_filename = secure_filename(image_two.filename)
                
                image_one.save(os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename))
                image_two.save(os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename))

                full_path_one = os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename)
                full_path_two = os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename)

                category = request.form.get('project_category')
                description = request.form.get('description')


                current_issues = issues.find()
                total_issue_count = current_issues.count()

                issue_id = str(total_issue_count + 1)

                date = str(datetime.date.today())
                timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))
                stamp = '%s-%s' % (date, timestamp)

                find_user = users.find_one({'name' : session['username']})

                proc = Processor()
                points, cam1, cam2 = proc.process_files(full_path_one, full_path_two, issue_id, find_user)

                lat, lon = proc.get_location_data(full_path_one)

                det, label = proc.get_damage_type(full_path_one, issue_id)

                geolocator = Nominatim(timeout=10)
                loc_string = "%s %s" % (lat, lon)
                location = geolocator.reverse(loc_string)
                
                locality = location.address
                add_s = locality.split(',')
                title_address = ' '.join(add_s[0:3])

                title = "%s at %s" % (label, title_address)

                points_curr = os.path.join(APP_ROOT, points)
                points_dest = os.path.join(app.config['PROCESSED_FOLDER'], points)

                volume = proc.get_volume(points_dest)

                cam1_curr = os.path.join(APP_ROOT, cam1)
                cam1_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam1)

                cam2_curr = os.path.join(APP_ROOT, cam2)
                cam2_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam2)

                det_curr = os.path.join(APP_ROOT, det)
                det_dest = os.path.join(app.config['UPLOAD_FOLDER'], det)

                # Move files to proper directories

                shutil.move(points_curr, points_dest)
                shutil.move(cam1_curr, cam1_dest)
                shutil.move(cam2_curr, cam2_dest)
                shutil.move(det_curr, det_dest)

                issues.insert({'issue_id': issue_id, 'category' : category, 'points' : points_dest, 'cam1' : cam1_dest, 
                    'cam2' : cam2_dest, 'locality': locality, 'image_one_full' : full_path_one, 'image_two_full' : full_path_two, 
                    'description' : description, 'uploaded_by': session['username'], 'time' : stamp, 'detection' : det_dest, 'lat' : lat, 'lon' : lon,
                    'label' : label, 'title' : title, 'status' : 'Not Fixed', 'volume' : volume})

                
                progress.insert({'issue_id' : issue_id, 'type' : 'created', 'time' : timestamp, 'date' : date, 'upload' : session['username']})

                iso_date = datetime.datetime.now().isoformat()
                references.insert({'date' : iso_date, 'issue_id' : issue_id, 'referenced_by' : session['username']})

                current_issue = issues.find_one({'issue_id' : issue_id})
                document_processor = DocumentManager()
                doc_uri = document_processor.generateDocTable(current_issue)
                documents.insert({'issue_id' : issue_id, 'doc_uri' : doc_uri})

                calculate_rank()
                
                return redirect('/app/issues/create')

        return render_template('pages/app/create.html')
    else:
        return redirect('/userlogin')


@app.route('/app/issues')
def existing_issues():
    if 'username' in session:
        issues = mongo.db.issues
        rankings = mongo.db.rankings

        now = datetime.datetime.now() + datetime.timedelta(seconds = 60 * 3.4)

        all_issues = issues.find({})
        
        all_issues_id = []
        all_issues_name = []
        all_issues_age = []
        all_issues_category = []
        all_issues_uploader = []
        all_issues_thumbnail = []
        all_issues_rank = []
        

        issuelist = range(0, all_issues.count(), 1)

        if all_issues.count() > 0:
            for issue in all_issues:
                find_rank = rankings.find_one({'issue_id' : issue['issue_id']})
                rank = find_rank['rank']
                all_issues_rank.append(rank)
                all_issues_id.append(issue['issue_id'])
                all_issues_name.append(issue['title'])
                all_issues_age.append(str(timeago.format(issue['time'], now)))
                all_issues_category.append(issue['category'])
                all_issues_uploader.append(issue['uploaded_by'])
                all_issues_thumbnail.append(str("/downloads/%s" % issue['image_one_full'].split('/')[-1]))
        

        return render_template('pages/app/issues.html', issuelist=issuelist, all_issues_id=all_issues_id, all_issues_age=all_issues_age,
            all_issues_name=all_issues_name, all_issues_category=all_issues_category, all_issues_uploader=all_issues_uploader, 
            all_issues_thumbnail=all_issues_thumbnail, all_issues_rank=all_issues_rank)
            
    else:
        return redirect('/userlogin')

@app.route('/app/issues/map')
def map_page():
    if 'username' in session:
        issues = mongo.db.issues
        now = datetime.datetime.now() + datetime.timedelta(seconds = 60 * 3.4)

        all_issues = issues.find({})
        
        all_issues_id = []
        all_issues_name = []
        all_issues_age = []
        all_issues_category = []
        all_issues_uploader = []
        all_issues_thumbnail = []
        all_issues_lat = []
        all_issues_long = []
        

        issuelist = range(0, all_issues.count(), 1)

        if all_issues.count() > 0:
            for issue in all_issues:
                all_issues_id.append(issue['issue_id'])
                all_issues_name.append(issue['title'])
                all_issues_age.append(str(timeago.format(issue['time'], now)))
                all_issues_category.append(issue['category'])
                all_issues_uploader.append(issue['uploaded_by'])
                all_issues_lat.append(issue['lat'])
                all_issues_long.append(issue['lon'])
                all_issues_thumbnail.append(str("/downloads/%s" % issue['image_one_full'].split('/')[-1]))
        

        return render_template('pages/app/map.html', issuelist=issuelist, all_issues_id=all_issues_id, all_issues_age=all_issues_age,
            all_issues_name=all_issues_name, all_issues_category=all_issues_category, all_issues_uploader=all_issues_uploader, 
            all_issues_thumbnail=all_issues_thumbnail, all_issues_lat=all_issues_lat, all_issues_long=all_issues_long)

    return redirect('/userlogin')

@app.route('/app/issues/map/view')
def map_view():
    if 'username' in session:
        issues = mongo.db.issues
        now = datetime.datetime.now() + datetime.timedelta(seconds = 60 * 3.4)

        all_issues = issues.find({})
        
        all_issues_id = []
        all_issues_name = []
        all_issues_age = []
        all_issues_category = []
        all_issues_uploader = []
        all_issues_thumbnail = []
        all_issues_lat = []
        all_issues_long = []
        

        issuelist = range(0, all_issues.count(), 1)

        if all_issues.count() > 0:
            for issue in all_issues:
                all_issues_id.append(issue['issue_id'])
                all_issues_name.append(issue['title'])
                all_issues_age.append(str(timeago.format(issue['time'], now)))
                all_issues_category.append(issue['category'])
                all_issues_uploader.append(issue['uploaded_by'])
                all_issues_lat.append(issue['lat'])
                all_issues_long.append(issue['lon'])
                all_issues_thumbnail.append(str("/downloads/%s" % issue['image_one_full'].split('/')[-1]))
        

        return render_template('location.html', issuelist=issuelist, all_issues_id=all_issues_id, all_issues_age=all_issues_age,
            all_issues_name=all_issues_name, all_issues_category=all_issues_category, all_issues_uploader=all_issues_uploader, 
            all_issues_thumbnail=all_issues_thumbnail, all_issues_lat=all_issues_lat, all_issues_long=all_issues_long)

    return redirect('/userlogin')

@app.route('/app/issues/view/<issue_id>')
def view_issue(issue_id):
    if 'username' in session:
        issues = mongo.db.issues
        comments = mongo.db.comments
        documents = mongo.db.documents
        progress = mongo.db.progress
        references = mongo.db.references

        issue = issues.find_one({'issue_id' : issue_id})
        issue_category = issue['category']
        issue_description = issue['description']
        issue_uploader = issue['uploaded_by']
        issue_time = issue['time']
        issue_image_one_full = issue['image_one_full']
        issue_image_two_full = issue['image_two_full']
        issue_points_full = issue['points']
        issue_cam1_full = issue['cam1']
        issue_cam2_full = issue['cam2']
        issue_det_full = issue['detection']

        issue_lat = str(issue['lat'])
        issue_lon = str(issue['lon'])
        issue_title = issue['title']
        issue_location = issue['locality']

        issue_points = "/cdn/pointcloud/%s" % issue_points_full.split('/')[-1]
        issue_cam1 = "/cdn/pointcloud/%s" % issue_cam1_full.split('/')[-1]
        issue_cam2 = "/cdn/pointcloud/%s" % issue_cam2_full.split('/')[-1]

        issue_image_one = "/downloads/%s" % issue_image_one_full.split('/')[-1]
        issue_image_two = "/downloads/%s" % issue_image_two_full.split('/')[-1]

        issue_image_det = "/downloads/%s" % issue_det_full.split('/')[-1]

        issue_document_find = documents.find_one({'issue_id' : issue_id})
        issue_document = issue_document_find['doc_uri']

        issue_volume = issue['volume']

        find_references_all = references.find({'issue_id' : issue_id})
        find_references_today = references.find({ 'date': { '$lte': datetime.datetime.now() - datetime.timedelta(hours=24)}})

        all_counts = find_references_all.count()
        all_today = find_references_today.count()

        previous = all_counts - all_today
        change_percent = ((float(all_counts)-previous)/previous)*100

        issue_comments = []

        find_comments = comments.find({'comment_issue' : issue_id})
        for comment in find_comments:
            comment_uploader = comment['comment_uploader']
            comment_id = comment['comment_id']
            comment_date = comment['comment_date']
            comment_time = comment['comment_timestamp']
            comment_votes = comment['comment_votes']
            comment_text = comment['comment_text']
            comment_props = {'uploader' : comment_uploader, 'id' : comment_id,
                'date' : comment_date, 'time' : comment_time, 'votes' : comment_votes,
                'text' : comment_text}
            
            issue_comments.append(comment_props)

        progress_reports = []
        find_progress_reports = progress.find({'issue_id' : issue_id})
        for progress in find_progress_reports:
            uploader = progress['upload']
            time = progress['time']
            date = progress['date']
            p_type = progress['type']
            issue_props = {'uploader' : uploader, 'time' : time, 'type' : p_type, 'date' : date}
            progress_reports.append(issue_props)

        print(progress_reports)


        return render_template('pages/app/single_issue.html', issue_id=issue_id, issue_category=issue_category, issue_description=issue_description,
            issue_uploader=issue_uploader, issue_time=issue_time, issue_points=issue_points, issue_cam1=issue_cam1, issue_cam2=issue_cam2,
            issue_image_one=issue_image_one, issue_image_two=issue_image_two, issue_lat=issue_lat, issue_lon=issue_lon, issue_title=issue_title,
            issue_image_det=issue_image_det, issue_location=issue_location, issue_comments=issue_comments, progress_reports=progress_reports,
            issue_volume=issue_volume, issue_document=issue_document, today_change=change_percent, issue_reported_number_today=all_counts)
    else:
        return redirect('/userlogin')


@app.route('/app/issues/feedback/<issue_id>', methods=['POST', 'GET'])
def feedback_upload(issue_id):
    if 'username' in session:
        comments = mongo.db.comments
        progress = mongo.db.progress
        if request.method == 'POST':
            image_file = request.files['file']
            if image_file and allowed_file(image_file.filename):
                image_filename = secure_filename(image_file.filename)
                image_file.save(os.path.join(app.config['UPLOAD_FOLDER'], image_filename))
                full_image_path = os.path.join(app.config['UPLOAD_FOLDER'], image_filename)
                date = str(datetime.date.today())
                timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))

                all_comments = comments.find({})
                all_comment_count = all_comments.count()

                comment_id = str(all_comment_count + 1)
                # comment_uploader = session['username']
                comment_votes = 0

                proc = Processor()
                det, label = proc.get_damage_type(full_image_path, issue_id)
                
                if len(label) == 0:
                    damage_status = 'fixed'
                else:
                    damage_status = 'not fixed'

                comment_text = '<p>A new image was uploaded by %s at (%s, %s). The damage was found to be <b>%s</b>. Kindly re-generate report for getting updated report.</p> <br> <img src="/downloads/%s" width=450/> ' % (session['username'], timestamp, date, damage_status, image_filename)

                comments.insert({'comment_id' : comment_id, 'comment_date' : date, 'comment_timestamp' : timestamp,
                'comment_text' : comment_text, 'comment_uploader' : 'Damage Control Bot', 'comment_issue' : issue_id, 'comment_votes' : comment_votes})

                progress.insert({'issue_id' : issue_id, 'type' : damage_status, 'time' : timestamp, 'date' : date, 'upload' : session['username']})

                redirect_to = '/app/issues/view/%s' % (issue_id)

            return redirect(redirect_to)
    else:
        return redirect('/userlogin')

@app.route('/app/issues/comments/upvote/<comment_id>')
def comment_upvote(comment_id):
    comments = mongo.db.comments
    current_comment = comments.find_one({'comment_id' : comment_id})
    comment_issue = current_comment['comment_issue']
    comment_votes = int(current_comment['comment_votes'])
    new_votes = int(comment_votes + 1)
    redirect_to = '/app/issues/view/%s' % (comment_issue)
    comments.update_one({'comment_id': comment_id}, {"$set": {'comment_votes': new_votes}})
    return redirect(redirect_to)

@app.route('/app/issues/comments/downvote/<comment_id>')
def comment_downvote(comment_id):
    comments = mongo.db.comments
    current_comment = comments.find_one({'comment_id' : comment_id})
    comment_issue = current_comment['comment_issue']
    comment_votes = int(current_comment['comment_votes'])
    new_votes = int(comment_votes - 1)
    redirect_to = '/app/issues/view/%s' % (comment_issue)
    comments.update_one({'comment_id': comment_id}, {"$set": {'comment_votes': new_votes}})
    return redirect(redirect_to)


@app.route('/app/issues/<issue_id>/add_comment', methods=['POST', 'GET'])
def add_comment(issue_id):
    if 'username' in session:
        if request.method == 'POST':
            comments = mongo.db.comments
            all_comments = comments.find({})
            all_comments_count = all_comments.count()
            comment_uploader = session['username']
            comment_date = str(datetime.date.today())
            comment_timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))
            comment_text = request.form.get('text')
            comment_votes = 0
            comment_id = str(all_comments_count + 1)
            comments.insert({'comment_id' : comment_id, 'comment_date' : comment_date, 'comment_timestamp' : comment_timestamp,
                'comment_text' : comment_text, 'comment_uploader' : comment_uploader, 'comment_issue' : issue_id, 'comment_votes' : comment_votes})

            redirect_to = '/app/issues/view/%s' % (issue_id)

            return redirect(redirect_to)


@app.route('/app/calculate_volume/<issue_id>')
def calculate_volume(issue_id):
    issues = mongo.db.issues
    current_issue = issues.find_one({'issue_id' : issue_id})
    current_issue_points = current_issue['points']
    proc = Processor()
    volume = proc.get_volume(current_issue_points)
    print(volume)
    return str(volume)

@app.route('/app/generate_report/<issue_id>')
def generate_report(issue_id):

    issues = mongo.db.issues
    current_issue = issues.find_one({'issue_id' : issue_id})
    document_processor = DocumentManager()

    document_processor.generateDocTable(current_issue)

    return redirect('/')


@app.route('/app/reference/<issue_id>')
def reference(issue_id):
    if 'username' in session:
        references = mongo.db.references
        progress = mongo.db.progress
        iso_date = datetime.datetime.now().isoformat()
        date = str(datetime.date.today())
        timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))
        references.insert({'date' : iso_date, 'issue_id' : issue_id, 'referenced_by' : session['username']})
        progress.insert({'issue_id' : issue_id, 'type' : 'referenced', 'time' : timestamp, 'date' : date, 'upload' : session['username']})
        calculate_rank()
        return redirect('/')


@app.route('/app/submit')
def app_submit():
    return redirect('/app/issues/create')


# Mobile API

@app.route('/mobile/issues')
def mobile_issues():
    issues = mongo.db.issues
    all_issues = issues.find({}, {'_id': False})
    return dumps(all_issues)


@app.route('/mobile/login', methods=['POST'])
def mobile_login():
    users = mongo.db.users
    login_user = users.find_one({'name': request.form['username']})
    f_x = float(request.form.get('f_x'))
    f_y = float(request.form.get('f_y'))

    print(login_user['name'])
    print(f_x)
    print(f_y)

    if login_user:
        if bcrypt.hashpw(request.form.get('password').encode('utf-8'), login_user['password']) == login_user['password']:
            # session['username'] = request.form['username']
            users.update_one({'name' : login_user['name']}, {"$set" : {'f_x' : f_x}})
            users.update_one({'name' : login_user['name']}, {"$set" : {'f_y' : f_y}})
            return json.dumps({'login' : 'successful'})

    return json.dumps({'login' : 'unsuccessful'})


@app.route('/mobile/all_issues')
def get_all_locations():
    issues = mongo.db.issues
    find_all_issues = issues.find({})
    lat_long_list = []

    for issue in find_all_issues:
        lat_long_list.append({'lat' : str(issue['lat']), 'lon' : str(issue['lon'])})

    return json.dumps(lat_long_list)


@app.route('/mobile/issues/create', methods=['POST', 'GET'])
def create_issue_mobile():

    issues = mongo.db.issues
    documents = mongo.db.documents
    progress = mongo.db.progress
    references = mongo.db.references
    users = mongo.db.users

    if request.method == 'POST':
        image_one = request.files['image_one']
        image_two = request.files['image_two']
        if (image_one and allowed_file(image_two.filename) and (image_two and allowed_file(image_two.filename))):
            image_one_filename = secure_filename(image_one.filename)
            image_two_filename = secure_filename(image_two.filename)
                
            image_one.save(os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename))
            image_two.save(os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename))

            full_path_one = os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename)
            full_path_two = os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename)

            category = request.form.get('project_category')
            description = request.form.get('description')


            current_issues = issues.find()
            total_issue_count = current_issues.count()

            issue_id = str(total_issue_count + 1)

            date = str(datetime.date.today())
            timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))
            stamp = '%s-%s' % (date, timestamp)

            find_user = users.find_one({'name' : request.form.get('username')})

            proc = Processor()
            points, cam1, cam2 = proc.process_files(full_path_one, full_path_two, issue_id, find_user)

            lat, lon = proc.get_location_data(full_path_one)

            det, label = proc.get_damage_type(full_path_one, issue_id)

            geolocator = Nominatim(timeout=10)
            loc_string = "%s %s" % (lat, lon)
            print(loc_string)
            location = geolocator.reverse(loc_string)
                
            locality = location.address
            add_s = locality.split(',')
            title_address = ' '.join(add_s[0:3])

            title = "%s at %s" % (label, title_address)

            points_curr = os.path.join(APP_ROOT, points)
            points_dest = os.path.join(app.config['PROCESSED_FOLDER'], points)

            volume = proc.get_volume(points_dest)

            cam1_curr = os.path.join(APP_ROOT, cam1)
            cam1_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam1)

            cam2_curr = os.path.join(APP_ROOT, cam2)
            cam2_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam2)

            det_curr = os.path.join(APP_ROOT, det)
            det_dest = os.path.join(app.config['UPLOAD_FOLDER'], det)

            # Move files to proper directories

            shutil.move(points_curr, points_dest)
            shutil.move(cam1_curr, cam1_dest)
            shutil.move(cam2_curr, cam2_dest)
            shutil.move(det_curr, det_dest)

            issues.insert({'issue_id': issue_id, 'category' : category, 'points' : points_dest, 'cam1' : cam1_dest, 
                'cam2' : cam2_dest, 'locality': locality, 'image_one_full' : full_path_one, 'image_two_full' : full_path_two, 
                'description' : description, 'uploaded_by': request.form.get('username'), 'time' : stamp, 'detection' : det_dest, 'lat' : lat, 'lon' : lon,
                'label' : label, 'title' : title, 'status' : 'Not Fixed', 'volume' : volume})

                
            progress.insert({'issue_id' : issue_id, 'type' : 'created', 'time' : timestamp, 'date' : date, 'upload' : request.form.get('username')})

            iso_date = datetime.datetime.now().isoformat()
            references.insert({'date' : iso_date, 'issue_id' : issue_id, 'referenced_by' : request.form.get('username')})

            current_issue = issues.find_one({'issue_id' : issue_id})
            document_processor = DocumentManager()
            doc_uri = document_processor.generateDocTable(current_issue)
            documents.insert({'issue_id' : issue_id, 'doc_uri' : doc_uri})

            calculate_rank()
                
            return json.dumps({'issue' : 'Created Successfully'})

        return render_template('pages/app/create.html')


@app.route('/mobile/profile/<username>')
def get_profile(username):

    users = mongo.db.users
    issues = mongo.db.issues
    references = mongo.db.references

    find_user = users.find_one({'name' : username})

    user_email = find_user['email']
    user_fname = find_user['fullname']
    
    issues_by_user = issues.find({'uploaded_by' : username})
    issue_count = issues_by_user.count()

    references_by_user = references.find({'referenced_by' : username})
    ref_count = references_by_user.count()

    return json.dumps({'username' : username, 'fullname' : user_fname, 'email' : user_email, 'issues' : str(issue_count), 
        'references' : str(ref_count)})


# User management
# Login and register 
@app.route('/register', methods=['POST', 'GET'])
def register():
    if 'username' in session:
        return redirect('/')
    if request.method == 'POST':
        users = mongo.db.users
        user_fname = request.form.get('name')
        # user_fname = request.form['name']
        user_email = request.form.get('email')
        existing_user = users.find_one({'name': request.form.get('username')})
        if existing_user is None:
            hashpass = bcrypt.hashpw(request.form.get('password').encode('utf-8'), bcrypt.gensalt())
            users.insert(
                {'fullname': user_fname, 'email': user_email, 'name': request.form.get('username'),
                 'user_type': 'worker', 'password': hashpass, 'f_x' : 0.89115971, 'f_y' : 1.18821287})
            session['username'] = request.form.get('username')
            return redirect('/')

        return 'A user with that Email id/username already exists'

    return render_template('pages/app/register.html')

@app.route('/login', methods=['POST'])
def login():
    users = mongo.db.users
    login_user = users.find_one({'name': request.form['username']})

    if login_user:
        if bcrypt.hashpw(request.form.get('password').encode('utf-8'), login_user['password']) == login_user[
            'password']:
            session['username'] = request.form['username']
            return redirect('/')

    return 'Invalid username/password combination'

@app.route('/userlogin', methods=['POST', 'GET'])
def userlogin():
    if 'username' in session:
        return redirect('/')

    return render_template('pages/app/login.html')

@app.route('/logout')
def logout():
    session.pop('username', None)
    return redirect('/')


# Download management

@app.route('/downloads/<filename>')
def downloads(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/cdn/pointcloud/<filename>')
def pointcloud_cdn(filename):
    return send_from_directory(app.config['PROCESSED_FOLDER'], filename)

@app.route('/documents/<filename>')
def document_cdn(filename):
    return send_from_directory(app.config['FINAL_DOC_FOLDER'], filename)

# Page Errors

@app.errorhandler(404)
def page_not_found(e):
    return render_template('pages/app/404.html'), 404


@app.errorhandler(500)
def page_unresponsive(e):
    return render_template('pages/examples/lock.html'), 500

# Extra functions
def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS


def calculate_rank():
    issues = mongo.db.issues
    references = mongo.db.references
    rankings = mongo.db.rankings
    find_all_issues = issues.find({})

    # delete previous rankings
    rankings.remove({})

    data = []

    for issue in find_all_issues:
        issue_id = issue['issue_id']
        find_all_references = references.find({'issue_id' : issue_id})
        reference_count = find_all_references.count()
        data.append((issue_id, reference_count))
    
    data.sort(key=lambda x: x[1], reverse=True)

    rank = 1
    for d in data:
        issue_id = d[0]
        rankings.insert({'issue_id' : issue_id, 'rank' : rank})
        rank = rank + 1
    
    return 'done'

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True)