import tensorflow as tf
import numpy as np
from PIL import Image
from matplotlib import pyplot as plt
from GPSPhoto import gpsphoto
import os
import sys
import cv2 

import sys
import tarfile
import zipfile

from collections import defaultdict
from io import StringIO

from utils import label_map_util

from utils import visualization_utils as vis_util

from pyntcloud import PyntCloud


examples_dir = os.path.dirname(__file__)
weights_dir = os.path.join(examples_dir,'','weights')
sys.path.insert(0, os.path.join(examples_dir, 'python'))

PATH_TO_CKPT =  './models/ssd_mobilenet_RoadDamageDetector.pb' 
PATH_TO_LABELS = './models/crack_label_map.pbtxt'
NUM_CLASSES = 8

from depthmotionnet.networks_original import *

class Processor:


    def prepare_input_data(self, img1, img2, data_format):
        """Creates the arrays used as input from the two images."""
        # scale images if necessary
        if img1.size[0] != 256 or img1.size[1] != 192:
            img1 = img1.resize((256,192))
        if img2.size[0] != 256 or img2.size[1] != 192:
            img2 = img2.resize((256,192))
        img2_2 = img2.resize((64,48))
            
        # transform range from [0,255] to [-0.5,0.5]
        img1_arr = np.array(img1).astype(np.float32)/255 -0.5
        img2_arr = np.array(img2).astype(np.float32)/255 -0.5
        img2_2_arr = np.array(img2_2).astype(np.float32)/255 -0.5
        
        if data_format == 'channels_first':
            img1_arr = img1_arr.transpose([2,0,1])
            img2_arr = img2_arr.transpose([2,0,1])
            img2_2_arr = img2_2_arr.transpose([2,0,1])
            image_pair = np.concatenate((img1_arr,img2_arr), axis=0)
        else:
            image_pair = np.concatenate((img1_arr,img2_arr),axis=-1)
        
        result = {
            'image_pair': image_pair[np.newaxis,:],
            'image1': img1_arr[np.newaxis,:], # first image
            'image2_2': img2_2_arr[np.newaxis,:], # second image with (w=64,h=48)
        }
        return result


    # 
    # DeMoN has been trained for specific internal camera parameters.
    #
    # If you use your own images try to adapt the intrinsics by cropping
    # to match the following normalized intrinsics:
    #
    #  K = (0.89115971  0           0.5)
    #      (0           1.18821287  0.5)
    #      (0           0           1  ),
    #  where K(1,1), K(2,2) are the focal lengths for x and y direction.
    #  and (K(1,3), K(2,3)) is the principal point.
    #  The parameters are normalized such that the image height and width is 1.
    #

    def process_files(self, image_one_path, image_two_path, stamp, user):
        # read data
        if tf.test.is_gpu_available(True):
            data_format='channels_first'
        else: # running on cpu requires channels_last data format
            data_format='channels_last'

        img1 = Image.open(image_one_path)
        img2 = Image.open(image_two_path)

        f_x = float(user['f_x'])
        f_y = float(user['f_y'])

        normalized_f_x = f_x / img1.size[0]
        normalised_f_y = f_y / img1.size[1]

        w1 = ((0.89115971)*(256))/normalized_f_x
        h1 = ((1.18821287)*(192))/normalised_f_y

        w1 = int(w1)
        h1 = int(h1)

        img1 = img1.resize((w1, h1))
        img2 = img2.resize((w1, h1))

        pothole_prefix = "pothole_%s_" % stamp
        pothole_points = pothole_prefix + 'points.ply'
        pothole_cam1 = pothole_prefix  + 'cam1.ply'
        pothole_cam2 = pothole_prefix + 'cam2.ply'

        input_data = self.prepare_input_data(img1,img2,data_format)

        gpu_options = tf.GPUOptions()
        gpu_options.per_process_gpu_memory_fraction=0.8
        session = tf.InteractiveSession(config=tf.ConfigProto(allow_soft_placement=True, gpu_options=gpu_options))

        # init networks
        bootstrap_net = BootstrapNet(session, data_format)
        iterative_net = IterativeNet(session, data_format)
        refine_net = RefinementNet(session, data_format)

        session.run(tf.global_variables_initializer())

        # load weights
        saver = tf.train.Saver()
        saver.restore(session,os.path.join(weights_dir,'demon_original'))


        # run the network
        result = bootstrap_net.eval(input_data['image_pair'], input_data['image2_2'])
        for i in range(3):
            result = iterative_net.eval(
                input_data['image_pair'], 
                input_data['image2_2'], 
                result['predict_depth2'], 
                result['predict_normal2'], 
                result['predict_rotation'], 
                result['predict_translation']
            )
        rotation = result['predict_rotation']
        translation = result['predict_translation']
        result = refine_net.eval(input_data['image1'],result['predict_depth2'])

        bilinear_distance = 10
        physical_depth = bilinear_distance*result['predict_depth0']


        plt.imshow(result['predict_depth0'].squeeze(), cmap='Greys')
        plt.show()

        # try to visualize the point cloud
        try:
            from depthmotionnet.vis import visualize_prediction, export_prediction_to_ply
            visualize_prediction(
                inverse_depth=result['predict_depth0'], 
                image=input_data['image_pair'][0,0:3] if data_format=='channels_first' else input_data['image_pair'].transpose([0,3,1,2])[0,0:3], 
                rotation=rotation, 
                translation=translation)

            export_prediction_to_ply(
                output_prefix=pothole_prefix,
                inverse_depth=result['predict_depth0'],
                image=input_data['image_pair'][0,0:3] if data_format=='channels_first' else input_data['image_pair'].transpose([0,3,1,2])[0,0:3],
                rotation=rotation,
                translation=translation)

        except ImportError as err:
            print("Cannot visualize as pointcloud.", err)

        return pothole_points, pothole_cam1, pothole_cam2


    def load_image_into_numpy_array(self, image):
        (im_width, im_height) = image.size
        return np.array(image.getdata()).reshape(
            (im_height, im_width, 3)).astype(np.uint8)

    
    def get_label(self, cat_index):
        categories = {'D00' : 'Wheel Marks', 'D01' : 'Longitudinal Construction Joint', 'D10' : 'Lateral Interval',
            'D11' : 'Lateral Construction Joint', 'D20' : 'Partial Pavement', 'D40' : 'Pothole', 'D43' : 'Cross Walk Blur', 'D44' : 'Shallow Pothole'}
        return categories[cat_index]


    def get_damage_type(self, image_one_full, stamp):        

        IMAGE_SIZE = (12, 8)
        # Load graph
        detection_graph = tf.Graph()

        pothole_file = "pothole_%s_det.jpg" % stamp

        with detection_graph.as_default():
            od_graph_def = tf.GraphDef()
            with tf.gfile.GFile(PATH_TO_CKPT, 'rb') as fid:
                serialized_graph = fid.read()
                od_graph_def.ParseFromString(serialized_graph)
                tf.import_graph_def(od_graph_def, name='')

        # Create label map
        label_map = label_map_util.load_labelmap(PATH_TO_LABELS)
        categories = label_map_util.convert_label_map_to_categories(label_map, max_num_classes=NUM_CLASSES, use_display_name=True)
        category_index = label_map_util.create_category_index(categories)

        with detection_graph.as_default():
            with tf.Session(graph=detection_graph) as sess:
                image_tensor = detection_graph.get_tensor_by_name('image_tensor:0')
                detection_boxes = detection_graph.get_tensor_by_name('detection_boxes:0')
                detection_scores = detection_graph.get_tensor_by_name('detection_scores:0')
                detection_classes = detection_graph.get_tensor_by_name('detection_classes:0')
                num_detections = detection_graph.get_tensor_by_name('num_detections:0')
                
                image = Image.open(image_one_full)
                image_np = self.load_image_into_numpy_array(image)
                image_np_expanded = np.expand_dims(image_np, axis=0)
                
                # Actual detection.
                (boxes, scores, classes, num) = sess.run(
                    [detection_boxes, detection_scores, detection_classes, num_detections],
                    feed_dict={image_tensor: image_np_expanded})
                

                # Visualization of the results of a detection.
                vis_util.visualize_boxes_and_labels_on_image_array(
                    image_np,
                    np.squeeze(boxes),
                    np.squeeze(classes).astype(np.int32),
                    np.squeeze(scores),
                    category_index,
                    min_score_thresh=0.3,
                    use_normalized_coordinates=True,
                    line_thickness=20)

                final_score = np.squeeze(scores)    
                objects = []
                for index, value in enumerate(classes[0]):
                    object_dict = {}

                    if scores[0, index] > 0.4:
                        object_dict[(category_index.get(value)).get('name')] = \
                                            scores[0, index]
                        objects.append(category_index.get(value).get('name'))

                print(objects)
                
                if len(objects) == 0:
                    objects.append('D20')
                
                if len(objects) > 0:

                    label = self.get_label(objects[0])
                    
                    plt.figure(figsize=IMAGE_SIZE)
                    plt.imshow(image_np)
                    plt.savefig(pothole_file)

                else:
                    label = ''

        return pothole_file, label

    def get_volume(self, point_cloud_file):
        shape = PyntCloud.from_file(point_cloud_file)
        convex_hull_id = shape.add_structure("convex_hull")
        convex_hull = shape.structures[convex_hull_id]
        shape.mesh = convex_hull.get_mesh()
        shape.to_file("diamond_hull.ply", also_save=["mesh"])
        volume = convex_hull.volume

        volume = volume * 10

        return volume

    def get_location_data(self, image_one_full):
        data = gpsphoto.getGPSData(image_one_full)
        lattitude = data['Latitude']
        longitude = data['Longitude']
        return lattitude, longitude
